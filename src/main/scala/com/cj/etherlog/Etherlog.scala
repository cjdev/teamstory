package com.cj.etherlog

import org.httpobjects.jetty.HttpObjectsJettyHandler
import org.httpobjects.HttpObject
import org.httpobjects.DSL._
import org.httpobjects.jackson.JacksonDSL._
import org.httpobjects.Request
import org.httpobjects.util.ClasspathResourcesObject
import org.httpobjects.util.ClasspathResourceObject
import java.io.InputStream
import java.io.InputStreamReader
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import org.codehaus.jackson.map.ObjectMapper
import com.codahale.jerkson.{Json => Jerkson}
import org.httpobjects.Representation
import java.io.OutputStream
import java.io.{File => Path}
import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.StringBuilder
import java.io.FileInputStream
import org.joda.time.DateMidnight
import org.joda.time.Instant
import com.cj.etherlog.api._
import com.cj.etherlog.chart._
import org.joda.time.Months

object Etherlog {
  
  def readAsStream(r:Representation) = {
    val bytes = new ByteArrayOutputStream()
    r.write(bytes);
    bytes.close()
    new ByteArrayInputStream(bytes.toByteArray())
  }
  
  def asString(is:InputStream):String = {
    val reader = new InputStreamReader(is)
    val buffer = new Array[Char](33)
    val text = new StringBuilder()
    
    var numRead = reader.read(buffer);
    while(numRead>=0){
       text.append(buffer, 0, numRead)
       numRead = reader.read(buffer);
    }
    text.toString()
  }
  
  def JerksonJson(o:AnyRef) = {
    new Representation(){
      override def contentType = "application/json"
      override def write(out:OutputStream){
        Jerkson.generate(o, out)
      }
    }
  }
  
  class Database[T](basePath:Path){
    basePath.mkdirs();
    
    def put(id:String, data:T):Unit  = this.synchronized{
      Jerkson.generate(data, pathFor(id))
    }
    
    def get(id:String)(implicit manifest:Manifest[T]):T = this.synchronized {
        val stream = new FileInputStream(pathFor(id))
        try {
            Jerkson.parse[T](stream)
        }finally{
          stream.close();
        }
        
    }
    
    def scan(fn:(String, T)=>Unit)(implicit manifest:Manifest[T]) {
      val files = basePath.listFiles()
      if(files!=null){
        files.foreach{file=>
          val id = file.getName()
          val value = get(id)
          fn(id, value)
        }
      }
    }
    
    def contains(id:String) = pathFor(id).exists()
    
    private def pathFor(id:String) = new Path(basePath, id);
  }
  
  
  def main(args: Array[String]) {
    
    val dataPath = new Path("data");
    
    val errors = new Database[String](new Path(dataPath, "errors"))
    val backlogs = new Database[BacklogStatus](new Path(dataPath, "backlogs"))
    val versions = new Database[BacklogVersion](new Path(dataPath, "versions"))
    
    if(!backlogs.contains("23")){
      val template = Jerkson.parse[Backlog](getClass().getResourceAsStream("/sample-data.js"))
      val initialBacklog = new Backlog(
                              id="23", 
                              name= template.name,
                              memo="initial sample version",
                              items = template.items)
      
      val initialVersion = new BacklogVersion(
                                  id = UUID.randomUUID().toString(),
                                  when = System.currentTimeMillis(),
                                  isPublished = true,
                                  previousVersion = null,
                                  backlog = initialBacklog
                              )
      
        backlogs.put(
                id="23", 
                data = new BacklogStatus(id="23", latestVersion = initialVersion.id))
        versions.put(initialVersion.id, initialVersion)
    }
    
    
    def scanBacklogHistory(backlogId:String, fn:(BacklogVersion)=>Unit) {
      val backlog = backlogs.get(backlogId);
              
      var nextVersionId = backlog.latestVersion
      while(nextVersionId!=null){
        val version = versions.get(nextVersionId)
        fn(version)
        nextVersionId = version.previousVersion
      }
    }
    
    def buildStatsLog(id:String, until:Long, includeCurrentState:Boolean = false)= {
      val backlog = backlogs.get(id) 
              val allResults = new ListBuffer[StatsLogEntry]()
              
              def incrementedEstimate(tally:Int, item:Item) = {
                  item.bestEstimate match {
                  case Some(value) => {
                      value + tally
                  }
                  case None => tally
                  }
              }
              
              scanBacklogHistory(id, {version=>
                    val items = version.backlog.items
                    val amountComplete:Int = items.filter(_.isComplete.getOrElse(false)).foldLeft(0)(incrementedEstimate);
                    val amountTodo:Int = items.filter(!_.isComplete.getOrElse(false)).foldLeft(0)(incrementedEstimate);
                    
                    allResults += StatsLogEntry(
                            version=version.id, 
                            when=version.when, 
                            memo=version.backlog.memo,
                            todo=amountTodo,
                            done=amountComplete)
                                
              }) 
              
              var results = allResults.filter(_.when<=until)
              
              var latest = results.first
              
              results.filter{item=> 
                    var includeBecauseItsLast = (includeCurrentState && item.version == latest.version)
                    var includeBecauseItsNotWIP = item.memo!="work-in-progress" 
                    includeBecauseItsLast || includeBecauseItsNotWIP
              }
              
    }
    
     class ChartStylesheet(parentPath:String) extends HttpObject(parentPath + "/mystyle.css"){
            override def get(req:Request) = {
              OK(FromClasspath("text/css", "/content/mystyle.css", getClass))
            }
        }
     
     def updateBacklog(newBacklogState:Backlog) = {
       
          val backlogId = this.synchronized{
            var candidate = 1
            while(backlogs.contains(candidate.toString())){
              candidate += 1;
            }
            candidate.toString()
          }
          
          val initialBacklog = Backlog(
              id=backlogId, 
              name=newBacklogState.name, 
              memo=newBacklogState.memo, 
              items = newBacklogState.items)
          val initialVersion = BacklogVersion(
                  id=UUID.randomUUID().toString(),
                  backlog = initialBacklog,
                  isPublished= false, 
                  previousVersion = null
          )
          
          versions.put(initialVersion.id, initialVersion)
          
          val status = BacklogStatus(
              id=backlogId,
              latestVersion = initialVersion.id
              )
          
          backlogs.put(backlogId, status)
          status.id
     }
    
     def buildStatsLogFromQueryString(id:String, req:Request) = {
      val endParam = req.getParameter("end")
      val showLatestEvenIfWipParam = req.getParameter("showLatestEvenIfWip")
      
      val end = if(endParam==null) System.currentTimeMillis() else endParam.toLong
      val showLatestEvenIfWip = if(showLatestEvenIfWipParam==null) false else showLatestEvenIfWipParam.toBoolean
      
      
      buildStatsLog(id=id, until=end, includeCurrentState = showLatestEvenIfWip);
      
     }
     
    val port = 43180
    
    HttpObjectsJettyHandler.launchServer(port, 
        new HttpObject("/api/backlogs"){
            override def get(req:Request) = {
               val results = new ListBuffer[BacklogListEntry]()
               
               backlogs.scan{(id, backlog)=>
                 val version = versions.get(backlog.latestVersion)
                 results += BacklogListEntry(id=id, name=version.backlog.name)
               }
              
               OK(JerksonJson(results))
            }
            
            override def post(req:Request) = {
              val backlogRecieved = Jerkson.parse[Backlog](readAsStream(req.representation()));
              
              val newVersionId = updateBacklog(backlogRecieved);
              
              CREATED(Location("/api/backlogs/" + newVersionId))
            }
        },
        new ChartStylesheet("/api/backlogs/{id}"),
        new ChartStylesheet("/backlog/"),
        new HttpObject("/api/backlogs/{id}/chart"){
            override def get(req:Request) = {
              val id = req.pathVars().valueFor("id")
              val stats = buildStatsLogFromQueryString(id, req);
              val nowParam = req.getParameter("now");
              val now = if(nowParam==null) System.currentTimeMillis() else nowParam.toLong
              
              val lastTime = now + (Months.months(3).toMutablePeriod().toDurationFrom(new Instant(now)).getMillis())
              println(new Instant(lastTime))
              
              val backlog = backlogs.get(id)
              val version = versions.get(backlog.latestVersion)
              
              val text = makeSvg(
                              stats=stats.toList.reverse, 
                              lastTime = lastTime, 
                              whenProjectedComplete = lastTime,
                              goals=version.backlog.goalLines
                         )
              
              OK(Bytes("image/svg+xml", text.getBytes()))
            }
            
        },
        new HttpObject("/api/backlogs/{id}/history"){
            override def get(req:Request) = {
              val id = req.pathVars().valueFor("id")
              val results = new ListBuffer[HistoryItem]()
              scanBacklogHistory(id, {version=>
                results += HistoryItem(version=version.id, when=version.when, memo=version.backlog.memo)
              }) 
              
              var savePoints = results.filter(item=>item.memo != "work-in-progress")
              OK(JerksonJson(savePoints))
            }
            
        },
        new HttpObject("/api/backlogs/{id}/statsLog"){
            override def get(req:Request) = {
              val id = req.pathVars().valueFor("id")
              val stats = buildStatsLogFromQueryString(id, req);
              
              OK(JerksonJson(stats))
            }
        },
        new HttpObject("/api/backlogs/{id}/history/{version}"){
            override def get(req:Request) = {
              val id = req.pathVars().valueFor("id")
              val versionId = req.pathVars().valueFor("version")
              val results = new ListBuffer[HistoryItem]()
              
              if(versions.contains(versionId)){
                val version = versions.get(versionId);
                  
                OK(JerksonJson(version.backlog))
              }else{
                NOT_FOUND(Text("No such version"))
              }
            }
            
        },
        new HttpObject("/api/backlogs/{id}"){
            override def get(req:Request) = {
              val id = req.pathVars().valueFor("id")
              val backlog = backlogs.get(id)
              val currentVersion = versions.get(backlog.latestVersion);
              OK(JerksonJson(currentVersion.backlog))
            }
            override def put(req:Request) = {
              val id = req.pathVars().valueFor("id")
              val newBacklog = Jerkson.parse[Backlog](readAsStream(req.representation()));
              val backlog = backlogs.get(id);
              
              val newVersion = new BacklogVersion(
                                  id = UUID.randomUUID().toString(),
                                  when = System.currentTimeMillis(),
                                  isPublished = false,
                                  previousVersion = backlog.latestVersion,
                                  backlog = newBacklog
                              )
              val updatedBacklog = new BacklogStatus(
                                          backlog.id, 
                                          latestVersion = newVersion.id)
              
              versions.put(updatedBacklog.latestVersion, newVersion);
              backlogs.put(id, updatedBacklog)
              get(req)
            }
        },
        new HttpObject("/api/errors"){
          
            override def post(req:Request) = {
              val errorId = UUID.randomUUID().toString();
              val error = asString(readAsStream(req.representation()))
              
              errors.put(errorId, error)
              
              CREATED(Location("/api/errors/" + errorId))
            }
        },
        new ClasspathResourceObject("/mockup", "/content/backlog-mockup.html", getClass()),
        new ClasspathResourceObject("/", "/content/index.html", getClass()),
        new ClasspathResourceObject("/backlog/{backlogId}", "/content/backlog.html", getClass()),
        new ClasspathResourcesObject("/{resource*}", getClass(), "/content")
    );
    
    println("etherlog is alive and listening on port " + port);
  }
}