package com.cj.etherlog.datas


import java.io.{File => Path}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.io.FileInputStream
import com.cj.etherlog.Jackson

class Database[T](basePath:Path){

    basePath.mkdirs();

    def put(id:String, data:T):Unit  = this.synchronized{
        Jackson.jackson.writeValue(pathFor(id), data)
    }

    def get(id:String)(implicit manifest:Manifest[T]):T = this.synchronized {
        val stream = new FileInputStream(pathFor(id))
        try {
            Jackson.parseJson(stream)
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