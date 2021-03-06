package org.teamstory.pivotal

import org.teamstory.Jackson
import com.fasterxml.jackson.annotation._
import scala.annotation.meta.getter
import org.apache.http.message.BasicHeader
import org.joda.time.DateTimeZone.UTC

class PivotalTrackerV5ApiStub(val authToken:String) {
  private def projectUrl(projectId:String) = s"https://www.pivotaltracker.com/services/v5/projects/${projectId}"
  
  val t = new JacksonHttpTool(new BasicHeader("X-TrackerToken", authToken))
  
  def getStories(projectId:String) = t.getJson[Array[PT5Story]](s"${projectUrl(projectId)}/stories")
  def getEpics(projectId:String) = t.getJson[Array[PT5Epic]](s"${projectUrl(projectId)}/epics")
  def getProject(projectId:String) = t.getJson[PT5Project](s"${projectUrl(projectId)}?fields=:default,current_velocity")
}

/**
 * A representation of a particular date and time. 
 * 
 * In general, datetime values can be expressed in one of two ways. They may be a string encoded according to the ISO 8601 
 * standard, like "2013-04-30T04:25:15Z". Or they will be an integer number of milliseconds since the beginning of the 
 * epoch, like 1367296015000. When supplying a parameter value that is a datetime, the client may use either format and 
 * Tracker will determine which is being used dynamically. 
 * 
 * By default, datetime values included in a response will be ISO 8601 strings. There are a handful of responses in which 
 * resources have datetime attributes which are only able to be formatted in one of these ways. This fact is noted in the 
 * descriptions of those particular attributes. For the majority, to cause dates to be formatted as millisecond offsets 
 * instead, supply the parameter date_format=millis in the query string of the request URL.
 */
object PT5DateTime {
  // e.g. 2014-11-06T04:47:06Z
  val pattern = org.joda.time.format.DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ss'Z'")
  @JsonCreator
  def deserialize(value:String) = new PT5DateTime(value)
}
case class PT5DateTime(@(JsonValue @getter) val value:String) {
  def millis:Long = PT5DateTime.pattern.parseDateTime(value).withZoneRetainFields(UTC).getMillis()
}


case class PT5TimeZone(
    /**
     * The Olson name for the time zone.
     */
    olson_name:String ,
    
    /**
     * The offset, from UTC, of the time zone. This is a string containing a formatted representation of the time zone offset. First, and optional + or - sign (no sign is equivalent to '+'), then a number of hours, a colon, and a number of minutes. Only valid, internationally-recognized time zone offsets should be used when sending zone information for the client. For example, "-01:03" and "+23:00" are not valid values, even though they match the encoding pattern.
     */
    offset:String,
    
    /**
     * The type of this object: time_zone. This field is read only.
     */
    kind:String
)


object PT5CalendarDay {
  @JsonCreator
  def deserialize(value:String) = new PT5CalendarDay(value)
}
case class PT5CalendarDay(@(JsonValue @getter) val value:String) {}

@JsonIgnoreProperties(ignoreUnknown=true)
case class PT5Label(
    
        /**
         * Database id of the label. This field is read only. This field is always returned.
         */
        id:Int, 
        /**
         * id of the project. This field is read only.
         */
        project_id:Int,
        /**
         * Required  —  The label's name.
         * string[255]
         */
        name:String,
        
        /**
         * Creation time. This field is read only.
         */
        created_at:PT5DateTime, 

        /**
         * Time of last update. This field is read only.
         */
        updated_at:PT5DateTime,
        
        /**
         * The type of this object: label. This field is read only. 
         */
        kind:String
)

@JsonIgnoreProperties(ignoreUnknown=true)
case class PT5Story(
    /**
     * Database id of the story. This field is read only. This field is always returned.
     */
    id:Int, 
    /**
     * id of the project
     */
    project_id:Int,
    /**
     * Required On Create  —  Name of the story. This field is required on create.
     *  string[5000]
     */
    name:String, 
    
    /**
     * In-depth explanation of the story requirements.
     * string[20000] 
     */
    description:String,
    
    /**
     * Type of story.
     *   enumerated string
     *   Valid enumeration values: feature, bug, chore, release
     */
    story_type:String,

    /**
     * Story's state of completion
     *  enumerated string 
     *  Valid enumeration values: accepted, delivered, finished, started, rejected, planned, unstarted, unscheduled
     */
    current_state:String,
    
    /**
     * estimate 
     * float
     * Point value of the story.
     */
    estimate:BigDecimal,
    
    /**
     * datetime 
     * Acceptance time.
     */
    accepted_at:PT5DateTime,
    
    /**
     * deadline datetime 
     * Due date/time (for a release-type story).
     * 
     */
    deadline:PT5DateTime,
    
    /**
     * The id of the person who requested the story. In API responses, this attribute may be requested_by_id or requested_by.
     */
    requested_by_id:Int,
    
    /**
     * The id of the person who owns the story. In API responses, this attribute may be owned_by_id or owned_by.
     */
    owned_by_id:Int,
    
    /**
     * IDs of the current story owners. By default this will be included in responses as an array of nested structures, using the key owners. In API responses, this attribute may be owner_ids or owners.
     */
    owner_ids:Seq[Int],
    
    /**
     * IDs of labels currently applied to story. By default this will be included in responses as an array of nested structures, using the key labels. In API responses, this attribute may be label_ids or labels.
     */
    label_ids:Seq[Int],
    labels:Seq[PT5Label],
    
    
    /**
     * IDs of tasks currently on the story. This field is writable only on create. This field is excluded by default. In API responses, this attribute may be task_ids or tasks.
     */
    task_ids:Seq[Int],
    
    /**
     * IDs of people currently following the story. This field is excluded by default. In API responses, this attribute may be follower_ids or followers.
     */
    follower_ids:Seq[Int],
    
    /**
     * IDs of comments currently on the story. This field is writable only on create. This field is excluded by default. In API responses, this attribute may be comment_ids or comments.
     */
    comment_ids:Seq[Int],
    
    /**
     * Creation time. This field is writable only on create.
     */
    created_at:PT5DateTime,
    
    /**
     * Time of last update. This field is read only.
     */
    updated_at:PT5DateTime,
    
    /**
     * ID of the story that the current story is located before. Null if story is last one in the project. This field is excluded by default.
     */
    before_id:Int,
    
    /**
     * ID of the story that the current story is located after. Null if story is the first one in the project. This field is excluded by default.
     */
    after_id:Int, 
    
    /**
     * ID of the integration API that is linked to this story. In API responses, this attribute may be integration_id or integration.
     */
    integration_id:Int,
  
    /**
     * The integration's specific ID for the story. (Note that this attribute does not indicate an association to another resource.)
     * string[255] 
     */
    external_id:String,
    
    /**
     * The url for this story in Tracker. This field is read only.
     */
    url:String, 

    /**
     * The type of this object: story. This field is read only.
     */
    kind:String
){
  
  def isComplete = state  match {
      case `accepted` => true
      case `delivered` => false
      case `finished` => true
      case `started` => false
      case `rejected` => false
      case `planned` => false
      case `unstarted` => false
      case `unscheduled` => false
    }
  
  def isInProgress = state  match {
      case `accepted` => false
      case `delivered` => false
      case `finished` => false
      case `planned` => false
      case `unstarted` => false
      case `unscheduled` => false
      case `started` => true
      case `rejected` => true
    }
  
  def state = PT5StoryState.valueOf(current_state).get
}

sealed trait PT5StoryState 
case object accepted extends PT5StoryState
case object delivered extends PT5StoryState
case object finished extends PT5StoryState
case object started extends PT5StoryState
case object rejected extends PT5StoryState
case object planned extends PT5StoryState
case object unstarted extends PT5StoryState
case object unscheduled extends PT5StoryState
object PT5StoryState{
  val values = Seq(
    accepted,
    delivered,
    finished,
    started,
    rejected,
    planned,
    unstarted,
    unscheduled)
    
    def valueOf(v:String):Option[PT5StoryState] = {
      val name = v + "$"
      values.find{v=>
          v.getClass().getSimpleName()==name
      }
    }
}


@JsonIgnoreProperties(ignoreUnknown=true)
case class PT5Epic (
    /**
     * Database id of the epic. This field is read only. This field is always returned.
     */
    id:Int,
    
    /**
     * id of the project.
     */
    project_id:Int,
    
    /**
     * name string[5000] 
     * Required On Create  —  Name of the epic. This field is required on create.
     */
    name:String,
    
    /**
     * id of the epic's label. By default this will be included in responses as a nested structure, using the key label. In API responses, this attribute may be label_id or label.
     */
    label_id:Int,
    label:PT5Label,
    
    /**
     *  In-depth explanation of the epic's goals, scope, etc.
     *  string[20000] 
     */
    description:String,

    /**
     * IDs of comments currently on the epic. This field is writable only on create. This field is excluded by default. In API responses, this attribute may be comment_ids or comments.
     */
    comment_ids:Seq[Int],

    /**
     * IDs of people currently following the story. This field is excluded by default. In API responses, this attribute may be follower_ids or followers.
     */
    follower_ids:Seq[Int],
    
    /**
     * Creation time. This field is read only.
     */
    created_at:PT5DateTime,
    
    /**
     * Time of last update. This field is read only.
     */
    updated_at:PT5DateTime,
    
    /**
     * The url for this epic in Tracker. This field is read only
     */
    url:String,
    
    /**
     * The type of this object: epic. This field is read only.
     */
    kind:String

)

@JsonIgnoreProperties(ignoreUnknown=true)
case class PT5Project (
    /**
     * Database id of the project. This field is read only. This field is always returned.
     */
    id:Int,
    
    /**
     * Required On Create  —  The name of the project. This field is required on create.
     * string[50] 
     */
    name:String,

    /**
     * A counter that is incremented each time something is changed within a project. The 
     * project version is used to track whether a client is 'up to date' with respect to 
     * the current content of the project on the server, and to identify what updates have 
     * to be made to the client's local copy of the project (if it stores one) to 
     * re-synchronize it with the server. 
     * 
     * This field is read only.
     */
    version:Int,
    
    /**
     * The number of weeks in an iteration.
     */
    iteration_length:Int,
    
    /**
     *  enumerated string 
     *    The day in the week the project's iterations are to start on.
     *    Valid enumeration values: Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
     */
    week_start_day:String,
    
    /**
     * The specification for the "point scale" available for entering story estimates within the project. It is specified as a comma-delimited series of values--any value that would be acceptable on the Project Settings page of the Tracker web application may be used here. If an exact match to one of the built-in point scales, the project will use that point scale. If another comma-separated point-scale string is passed, it will be treated as a "custom" point scale. The built-in scales are "0,1,2,3", "0,1,2,4,8", and "0,1,2,3,5,8".
     * string[255]
     */
    point_scale:String, 
    
    /**
     * True if the value of the point_scale string represents a custom, user-defined point scale rather than one of the ones built into Pivotal Tracker. This is important because of restrictions on moving stories from projects using a custom point scale into one using a standard point scale. Note that the set of built-in point scales is not considered part of the definition of an API version. Clients should be capable of processing any point_scale string that adheres to the format described above, and rely on this flag (rather than any explicit list that the client contains) to determine whether the project's point_scale is custom or standard. This field is read only.
     */
    point_scale_is_custom:Boolean,
    
    
    /**
     * When true, Tracker will allow estimates to be set on Bug- and Chore-type stories. This is strongly not recommended. Please see the FAQ for more information.
     */
    bugs_and_chores_are_estimatable:Boolean,

    /**
     *  When false, Tracker suspends the emergent planning of iterations based on the project's velocity, and allows users to manually control the set of unstarted stories included in the Current iteration. See the FAQ for more information.
     */ 
    automatic_planning:Boolean, 

    
    /**
     * When true, Tracker allows users to follow stories and epics, as well as use @mentions in comments. This field is read only.
     */
    enable_following:Boolean,
    
    /**
     * When true, Tracker allows individual tasks to be created and managed within each story in the project.
     */
    enable_tasks:Boolean,
    
    /**
     * The first day that should be in an iteration of the project. If both this and "week_start_day" are supplied, they must be consistent. It is specified as a string in the format "YYYY-MM-DD" with "01" for January. If this is not supplied, it will remain blank (null), but "start_time" will have a default value based on the stories in the project.
     */
    start_date:PT5CalendarDay,
    
    /**
     * The "native" time zone for the project, independent of the time zone(s) from which members of the project view or modify it.
     * time_zone
     */
    time_zone:PT5TimeZone,
    
    /**
     * The number of iterations that should be used when averaging the number of points of Done stories in order to compute the project's velocity.
     */
    velocity_averaged_over:Int,

    /**
     * The start time of the first iteration for which stories will be returned as part of the project, see 'number_of_done_iterations_to_show'. This field is read only. This field is excluded by default.
     */
    shown_iterations_start_time:PT5DateTime,

    /**
     * The computed start time of the project, based on the other project attributes and the stories contained in the project. If they are provided, the value of start_time will be based on week_start_day and/or start_date. However, if the project contains stories with accepted_at dates before the time that would otherwise be computed, the value returned in start_time will be adjusted accordingly. This field is read only.
     */
    start_time:PT5DateTime,
    
    /**
     *  There are areas within the Tracker UI and the API in which sets of stories automatically exclude the Done stories contained in older iterations. For example, in the web UI, the DONE panel doesn't necessarily show all Done stories by default, and provides a link to click to cause the full story set to be loaded/displayed. The value of this attribute is the maximum number of Done iterations that will be loaded/shown/included in these areas.
     */ 
    number_of_done_iterations_to_show:Int, 
    
    /**
     *  When true, the project has been associated with a Google Apps domain. Unless this is true, the /projects/{project_id}/google_attachments endpoint and the google_attachment resource cannot be used. This field is read only.
     */ 
    has_google_domain:Boolean,
    
    /**
     * A description of the project's content. Entered through the web UI on the Project Settings page.
     * string[140]
     */
    description:String, 
    
    /**
     * A long description of the project. This is displayed on the Project Overview page in the Tracker web UI.
     * string[65535]
     */
    profile_content:String,
    
    /**
     * When true, the project will accept incoming email responses to Tracker notification emails and convert them to comments on the appropriate stories.
     */
    enable_incoming_emails:Boolean,
    

    /**
     * The number which should be used as the project's velocity when there are not enough recent iterations with Done stories for an actual velocity to be computed.
     */
    initial_velocity:Int,
    
    /**
     * When true, Tracker will allow any user on the web to view the content of the project. The project will not count toward the limits of a paid subscription, and may be included on Tracker's Public Projects listing page.
     */
    public:Boolean,
    
    /**
     * When true, Tracker allows people to subscribe to the Atom (RSS, XML) feed of project changes
     */
    atom_enabled:Boolean,
    
    /**
     * Current iteration number for the project. This field is read only.
     */
    current_iteration_number:Int,
    
    /**
     * Current velocity for the project. This field is read only. This field is excluded by default.
     */
    current_velocity:Option[Int], 
    
    /**
     * Relative standard deviation of the points (adjusted for team strength and iteration length) completed over the number iterations used to compute velocity. This field is read only. This field is excluded by default.
     */
    current_volatility:BigDecimal,

    /**
     * The ID number for the account which contains the project.
     */
    account_id:Int,
    
    /**
     * One of the defined accounting types. This field is excluded by default.
     *    enumerated string
     *    Valid enumeration values: unbillable, billable, overhead
     */
    accounting_type:String,
    
    /**
     * Whether or not the project will be included on Tracker's Featured Public Projects web page. This field is excluded by default.
     */
    featured:Boolean,
    
    /**
     * IDs of stories currently in the project. It is possible that not all stories in the 'accepted' state will be included in this list. Only those stories accepted since the begining of a particular done iteration will be returned. This is controlled by an entry on the project's Settings page in the Tracker web user interface, and the state of that entry is reflected in the number_of_done_iterations_to_show property of the project. This property contains a number of iterations. Tracker counts back this number of iterations prior to the present Current iteration, and will not include stories from Done iterations prior to this group. To access these stories, use the GET /projects/##/stories endpoint. This field is read only. This field is excluded by default. In API responses, this attribute may be story_ids or stories.
     */
    story_ids:Seq[Int],
    
    /**
     * IDs of epics currently in the project. This field is read only. This field is excluded by default. In API responses, this attribute may be epic_ids or epics.
     */
    epic_ids:Seq[Int],
    
    /**
     * IDs of the exising memberships. This field is read only. This field is excluded by default. In API responses, this attribute may be membership_ids or memberships.
     */
    membership_ids:Seq[Int],
    
    /**
     * IDs of labels currently in the project. This field is read only. This field is excluded by default. In API responses, this attribute may be label_ids or labels.
     */
    label_ids:Seq[Int], 
    
    /**
     * IDs of integrations currently configured for the project. Note that integration information must be retrieved by getting project information with integrations included as a nested resource; there is currently no independent RESTy endpoint for accessing integrations. This field is read only. This field is excluded by default. In API responses, this attribute may be integration_ids or integrations.
     */
    integration_ids:Seq[Int],
    
    /**
     * IDs of iteration overrides currently configured for the project. Note that iteration override information must be retrieved by getting project information with iteration overrides included as a nested resource; there is currently no independent RESTy endpoint for accessing iteration overrides, but there is one for iterations, which contains the same info plus additional dynamic fields related to emergent iteration calculation. This field is read only. This field is excluded by default. In API responses, this attribute may be iteration_override_numbers or iteration_override_numbers.
     */
    iteration_override_numbers:Seq[Int],
    
    /**
     * Creation time. This field is read only.
     */
    created_at:PT5DateTime,
   
    /**
     * Time of last update. This field is read only.
     * datetime
     */
    updated_at:PT5DateTime,
    
    /**
     * The type of this object: project. This field is read only.
     */
    kind:String
)