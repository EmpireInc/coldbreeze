package controllers
import play.api._
import play.api.mvc._
import play.twirl.api.Html

//{Action, Controller}

import models.DAO.resources.{BusinessDAO, BusinessDTO}
import models.DAO._

import play.api._
import play.api.mvc._
import play.twirl.api.Html

//{Action, Controller}
import play.api.http.MimeTypes
import play.api.libs.json._
import play.api.cache._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats
import play.api.data.format.Formatter
import play.api.data.FormError


import views._
import models.User
import service.DemoUser
import securesocial.core._
import models.DAO._
import models.DAO.resources._
import main.scala.utils.{InputParamProc, ReactionActivator}
import scala.concurrent._
import ExecutionContext.Implicits.global
/**
 * Created by Sobolev on 22.07.2014.
 */
class ProcessInputController(override implicit val env: RuntimeEnvironment[DemoUser]) extends Controller with securesocial.core.SecureSocial[DemoUser] {
  import play.api.Play.current
  import models.DAO.CompositeValues
  implicit val CompositeVReads = Json.reads[CompositeValues]
  implicit val CompositeVWrites = Json.format[CompositeValues]
  implicit val stationReads = Json.reads[BPStationDTO]
  implicit val stationWrites = Json.format[BPStationDTO]

  implicit val logReads = Json.reads[BPLoggerDTO]
  implicit val logWrites = Json.format[BPLoggerDTO]
  implicit val BPSpaceReads = Json.reads[BPSpaceDTO]
  implicit val BPSpaceWrites = Json.format[BPSpaceDTO]
  implicit val SpaceElementReads = Json.reads[SpaceElementDTO]
  implicit val SpaceElementWrites = Json.format[SpaceElementDTO] 
  implicit val UndefElementReads = Json.reads[UndefElement]
  implicit val UndefElementWrites = Json.format[UndefElement]
  implicit val InputParamReads = Json.reads[InputParams]
  implicit val InputParamWrites = Json.format[InputParams]
  implicit val ReactionActivatorReads = Json.reads[ReactionActivator]
  implicit val ReactionActivatorWrites = Json.format[ReactionActivator]
  implicit val inLoggerReads = Json.reads[InputLogger]
  implicit val inLoggerWrites = Json.format[InputLogger]

  def invoke(bpID: Int)  = SecuredAction { implicit request =>
    if (security.BRes.procIsOwnedByBiz(request.user.businessFirst, bpID)) {
      val userId = request.user.main.userId
      val lang:String = models.AccountsDAO.getRolesAndLang(userId).get._3

      service.Build.run(bpID, Some(lang), invoke = true) match {
        case Some(process) => { 
          action(request.user.main.userId, process = Some(bpID), ProcHisCom.processLaunched, None, None)
          Ok(Json.toJson(Map("success" -> "station_id", "session" -> process.session_id.toString)))
        }
        case _ => Ok(Json.toJson(Map("error" -> "Error output")))
      }
    } else { Forbidden(Json.obj("status" -> "Access denied")) }  
  }
  def invokeFrom(session_id: Int, bpID: Int) = SecuredAction(BodyParsers.parse.json) { implicit request =>
    if (security.BRes.procIsOwnedByBiz(request.user.businessFirst, bpID)) {

      val pmsResult = request.body.validate[List[ReactionActivator]] 

    /*
    case class InputLogger(var id: Option[Int], 
      uid:Option[String]=None, 
      action:String, 
      arguments:List[String], 
      front_elem_id:Option[Int], 
      space_elem_id:Option[Int], 
      date: org.joda.time.DateTime,
      station: Int)*/
     // TODO: Input logger for reaction
    val input_logs = pmsResult.map{
                              case entity => entity.map { pm =>
                                  InputLogger(None, 
                                    uid = request.user.main.email, 
                                    action = "input", // TODO: Add reaction title
                                    arguments = List.empty[String], 
                                    reaction = pm.reaction_id, 
                                    input = None, 
                                    org.joda.time.DateTime.now,
                                    session_id)
                                }
      }
   
    if (controlles.launches.LaunchStack.push(launchId = session_id)) {
    InputLoggerDAO.pull_for_input(input_logs.get)
        // case class InputParamProc(felem: Option[Int], selem: Option[Int], param: String, args: List[String])
       /*
        val genparams = pmsResult.map{ 
          case entity => { 
               entity.map { t =>
                InputParamProc(t.f_elem, t.sp_elem, t.param, t.arguments.getOrElse(List.empty[String]))
                } 
          }
        }*/        
    service.Build.newRunFrom(session_id = session_id,bpID = bpID, params = pmsResult.get, invoke = true) match {
      case Some(process) => {
       action(request.user.main.userId, process = Some(bpID), ProcHisCom.processResumed, None, None)
       Ok(Json.toJson(Map("success" -> process.session_id)))
      }
      case _ => BadRequest(Json.toJson(Map("error" -> "Error output")))
    }
   } else { // already launching
      BadRequest(Json.toJson(Map("error" -> "Already launching")))
   }

  } else { Forbidden(Json.obj("status" -> "Access denied")) }
}

def inputLogs(BPid: Int) = Action { implicit request =>
    Ok(Json.toJson(InputLoggerDAO.getByBP(BPid)))
} 
def inputLogsBySession(BPid: Int, session_id:Int) = Action { implicit request =>
    Ok(Json.toJson(InputLoggerDAO.getBySession(session_id)))
} 

def schemes(BPid: Int, station_id: Int) = SecuredAction { implicit request =>
    if (security.BRes.procIsOwnedByBiz(request.user.businessFirst, BPid)) {


    val logs = BPLoggerDAO.findByStation(station_id)
    val elem_logs_ids = logs.diff(List(logs.last)).filter(log => log.element.isDefined).map(_.element)
    val space_logs_ids = logs.diff(List(logs.last)).filter(log => log.space_elem.isDefined).map(_.space_elem)
    // USER PERM IF ACTOR
    val owner_email = BPDAO.findOwnerByBP(BPid)
    val admins:List[String] = List(owner_email)//Employee.findAdminByBP(BPid) 
    val actors = EmployeeDAO.getAllByMaster(owner_email)
    val permited_elems = ActPermissionDAO.getByUID(request.user.main.email.get)

    if (isActor(request.user.main.email.get, actors)) {
    Ok(
      Json.toJson(
        Map("proc_elems" -> Json.toJson(ProcElemDAO.findByBPId(BPid).filter(elem => !elem_logs_ids.contains(elem.id) && permited_elems.flatMap(_.front_elem_id).contains(elem.id)   ) ), 
            "space_elems" -> Json.toJson(SpaceElemDAO.findByBPId(BPid).filter(elem => !space_logs_ids.contains(elem.id) && permited_elems.flatMap(_.space_elem_id).contains(elem.id)   ) ))
        )
      )
    }
    if (isAdmin(request.user.main.email.get, admins)) {
      Ok(
      Json.toJson(
        Map("proc_elems" -> Json.toJson(ProcElemDAO.findByBPId(BPid).filter(elem => !elem_logs_ids.contains(elem.id)  ) ), 
            "space_elems" -> Json.toJson(SpaceElemDAO.findByBPId(BPid).filter(elem => !space_logs_ids.contains(elem.id) ) ))
        )
      )
    }
    else {
      BadRequest(Json.toJson(Map("error" -> "Forbidden")))
    }
} else { Forbidden(Json.obj("status" -> "Access denied")) }  
}

def isActor(email:String, actors: List[EmployeeDTO]):Boolean = {
    actors.map(_.master_acc).contains(email)
}
def isAdmin(email:String, admins: List[String]):Boolean = {
    admins.contains(email)
}
private def action(acc: String, process: Option[Int], action: String, what: Option[String]=None, what_id: Option[Int]=None):Future[Int] = {
  Future {
      ProcHistoryDAO.pull_object(ProcessHistoryDTO(
        None, acc, action, process, what, what_id, org.joda.time.DateTime.now() ))
    }
}

/**
* Halt
*/
}

case class InputParams(f_elem:Option[Int] = None,  
                       sp_elem:Option[Int] = None,
                       param:String, 
                       arguments:Option[List[String]] = None)


