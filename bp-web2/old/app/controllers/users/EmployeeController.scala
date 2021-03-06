package controllers.users
import utils.auth.DefaultEnv

import models.DAO.resources.{EmployeeDTO, EmployeeDAO}
import models.DAO.resources.EmployeesBusinessDAO
import models.DAO.resources.BusinessDAO
import play.api._
import play.api.mvc._
import play.twirl.api.Html

//{Action, Controller}
import play.api.http.MimeTypes
import play.api.libs.json._
import play.api.cache._
import play.api.data._
import play.api.data.Forms._
import java.util.UUID

import views._
import controllers._
import models.Page
import models.User
import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import forms._
import models.User2
import play.api.i18n.{ I18nSupport, MessagesApi }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import models.DAO.resources.{AccoutGroupDTO,AccountGroupDAO,GroupDTO,GroupsDAO}
import models.DAO.resources.ClientBusinessDAO
import models.DAO.resources.web._

import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Awaitable, Await, Future}
import utilities.AccountCredHiding

/**
 * Created by Sobolev on 22.07.2014.
 */
case class ParticipatorsContainer(emps: List[EmployeeDTO], creds:List[models.daos.DBUser])


import javax.inject.Inject
import play.api.libs.mailer._

import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import forms._
import models.User2
import play.api.i18n.{ I18nSupport, MessagesApi }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.mvc.{ Action, RequestHeader }
class EmployeeController @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
    mailerClient: MailerClient,
  socialProviderRegistry: SocialProviderRegistry)
  extends Controller with I18nSupport {
   import play.api.Play.current
  val Home = Redirect(routes.EmployeeController.index())

  case class UIDS(emails: List[String], manager: Boolean)
  implicit val UIDSReads = Json.reads[UIDS]
  implicit val UIDSWrites = Json.format[UIDS]
  implicit val InputParamReads = Json.reads[EmployeeDTO]
  implicit val InputParamWrites = Json.format[EmployeeDTO]
  implicit val AccountReads = Json.reads[models.daos.DBUser]
  implicit val AccountWrites = Json.format[models.daos.DBUser]
  implicit val ParticipatorsContainerReads = Json.reads[ParticipatorsContainer]
  implicit val ParticipatorsContainerWrites = Json.format[ParticipatorsContainer]

   //val uidsForm = Form()
   // case class EmployeeDTO(var id: Option[Int], uid: String, acc:Int, position:Option[String], manager:Boolean = false)
   val employeeForm = Form(
    mapping(
      "id" -> optional(number),
      "uid" -> nonEmptyText,
      "master_acc" -> nonEmptyText,
      "position" -> optional(text),
      "manager" -> boolean,
      "workbench_id" -> number)(EmployeeDTO.apply)(EmployeeDTO.unapply))

 def index() = silhouette.SecuredAction.async { implicit request =>
      val business = request.identity.businessFirst
      if (business < 1) {
        Future(Redirect(controllers.routes.SettingController.workbench()))
      }
      else {

      val user = request.identity
      val employeesF:Future[Seq[EmployeeDTO]] = models.DAO.resources.EmployeeDAOF.getAllByWorkbench(request.identity.businessFirst)
      employeesF.flatMap { employees =>
        //val employees = models.DAO.resources.EmployeeDAOF.await(employeesF)


      val accounts = models.AccountsDAO.findAllByEmails(employees.toList.map(emp => emp.uid))
      val businessF = models.DAO.resources.BusinessDAOF.get(business)
      businessF.flatMap { businessReal =>

      val businesses = List(businessReal).flatten
      val assign = businesses.filter(buss => !employees.map(_.workbench).contains(buss.id.get) )
      val assigned = businesses.filter(buss => employees.map(_.workbench).contains(buss.id.get) )
      val groups:Future[Seq[GroupDTO]] = models.DAO.resources.GroupDAOF.getAllByBusiness(business)

      //val current_plan =
      val accPlanF = models.DAO.resources.AccountPlanDAOF.getByWorkbenchAcc(business)
      accPlanF.flatMap { accplan =>
        val aval = accplan match {
          case Some(acplan) => {
            println(acplan)
            (acplan.limit + 1) - employees.length
          }
          case _ => 0
        }
      // current employee limit + main manager MINUS all employee length for that master
        groups.map { groups =>
          Ok(views.html.businesses.users.employees(
            Page(employees.toList, 1, 1, employees.length), accounts, 1, "%", assign, assigned, groups.toList, aval)(user))
          }
      }
      }
      }
      }
}
 def team() = silhouette.SecuredAction.async { implicit request =>
      val business = request.identity.businessFirst
      if (business < 1) {
        Future(Redirect(controllers.routes.SettingController.workbench()))
      }
      else {
      val user = request.identity
      val employeesF:Future[Seq[EmployeeDTO]] = models.DAO.resources.EmployeeDAOF.getAllByWorkbench(request.identity.businessFirst)
      employeesF.map { employees =>
      val accounts = models.AccountsDAO.findAllByEmails(employees.toList.map(emp => emp.uid))
      val businesses = List(BusinessDAO.get(business).get)
      val ebs = EmployeesBusinessDAO.getAll
      val assign = businesses.filter(buss => !employees.map(_.workbench).contains(buss.id.get) )
      val assigned = businesses.filter(buss => employees.map(_.workbench).contains(buss.id.get) )


      //val master = EmployeeDAO.getMasterByAccount(user.email.get).get
      val team = businesses.find(biz => biz.id.getOrElse(-1) == business)

      val current_plan = models.DAO.resources.AccountPlanDAO.getByMasterAcc(user.email.get).get
      val aval = (current_plan.limit + 1) - employees.length
      // current employee limit + main manager MINUS all employee length for that master
      Ok(views.html.businesses.users.team(
        Page(employees.toList, 1, 1, employees.length), accounts, 1, "%", assign, assigned, team, aval)(user))
      }
  }
}
  def index_group(group_id: Int) = silhouette.SecuredAction.async { implicit request =>
      val business = request.identity.businessFirst
      if (business < 1) {
        Future(Redirect(controllers.routes.SettingController.workbench()))
      }
      else {
      val user = request.identity
      val grouped_employees = AccountGroupDAO.getAllByGroup(group_id).map(_.employee_id)
      val employeesF:Future[Seq[EmployeeDTO]] = models.DAO.resources.EmployeeDAOF.getAllByWorkbench(request.identity.businessFirst)
      employeesF.map { all_employees =>
      val employees = all_employees.toList.filter(emp => grouped_employees.contains(emp.id.get))
      val employees_ungrouped = all_employees.toList.filter(emp => !grouped_employees.contains(emp.id.get))

      val accounts = models.AccountsDAO.findAllByEmails(all_employees.toList.map(emp => emp.uid))
      println("Accounts: ")
      employees.map(emp => println(emp.uid))
      println(accounts)


      val businesses = List(BusinessDAO.get(business).get)
      val assign = businesses.filter(buss => !employees.map(_.workbench).contains(buss.id.get) )
      val assigned = businesses.filter(buss => employees.map(_.workbench).contains(buss.id.get) )

      val groups: List[GroupDTO] = GroupsDAO.getAllByBusiness(business)

      Ok(views.html.businesses.users.employees_group(
        Page(employees, 1, 1, employees.length), accounts, 1, "%", assign, assigned, groups, employees_ungrouped)(user))
    }
  }
}
//GET         /participators      @controllers.users.EmployeeController.participators()
def participators() = silhouette.SecuredAction.async { implicit request =>
     val user                        = request.identity.emailFilled
     val business                    = request.identity.businessFirst

     val employeesF                  = models.DAO.resources.EmployeeDAOF.getAllByWorkbench(business)
     employeesF.map { employees =>
       val employeesList = employees.toList
       val creds:List[models.daos.DBUser]      =  models.AccountsDAO.findAllByEmails(employeesList.map(_.master_acc))
       val cleanCreds = creds.map(acc => AccountCredHiding.hide(acc))

       Ok(Json.toJson(ParticipatorsContainer(employeesList, cleanCreds)))
     }
}


def create() = silhouette.SecuredAction { implicit request =>
        Ok(views.html.businesses.users.employee_form(employeeForm, request.identity))
}
def create_new() = silhouette.SecuredAction(BodyParsers.parse.json) { implicit request =>
  val workbench = request.identity.businessFirst
  if (isEmployeeOwned(request.identity.emailFilled, workbench)) {
    request.body.validate[UIDS].map{
      case entity => { entity.emails.map {
            e => EmployeeDTO(None, e, request.identity.emailFilled, None, entity.manager, workbench = workbench)
      }.map{ emp =>
        val employee_id = EmployeeDAO.pull_object_for(emp, request.identity.emailFilled)
        EmployeesBusinessDAO.pull(employee_id = employee_id, business_id = request.identity.businessFirst)
        // TODO SEND SIGN UP
        ///////??????????
        val msg = s"""<html><body>
            <p>
Hi! Good news. Someone invited to you into Minority workflow platform.</p>
<p>Let me remind you, Minority is a workflow management platform for small and medium businesses.
<a href="https://min.ority.us/auth/signUp">Go now</a><a href="x">x</a>. 
            </p>
        </body></html>"""
        Mailer.sendEmail(emp.uid, msg, mailerClient)
      }
      Ok(Json.toJson(Map("success" -> Json.toJson(entity))))
      }
    }.recoverTotal{
      e => BadRequest(Json.toJson(Map("xx" -> 1)))
    }
  } else { Home }
}
def update(id: Int) = silhouette.SecuredAction { implicit request =>
      val employee = EmployeeDAO.get(id)
      employee match {
        case Some(employee) =>
         Ok(views.html.businesses.users.employee_edit_form(id, employeeForm.fill(employee), request.identity))
        case None => Ok("not found")
      }
}
def update_make(id: Int) = silhouette.SecuredAction { implicit request =>
    if (isEmployeeOwned(request.identity.emailFilled, request.identity.businessFirst)) {

      employeeForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.businesses.users.employee_edit_form(id, formWithErrors, request.identity)),
        entity => {
          Home.flashing(EmployeeDAO.update(id,entity) match {
            case 0 => "failure" -> s"Could not update entity ${entity.uid}"
            case _ => "success" -> s"Entity ${entity.uid} has been updated"
          })
        })
    } else { Home }
}
def assign_business(employee_id: Int, business_id: Int) = silhouette.SecuredAction { implicit request =>
      if (isEmployeeOwned(request.identity.emailFilled, request.identity.businessFirst)) {
        val business = BusinessDAO.get(business_id)
        business match {
          case Some(business) =>
            EmployeesBusinessDAO.pull(employee_id = employee_id, business_id = business_id)
            Home.flashing("success" -> s"Employee $employee_id was assigned")
          case None => Home.flashing("failure" -> s"Business with $business_id not found")
        }
      } else { Home }
}
def unassign_business(employee_id: Int, business_id: Int) = silhouette.SecuredAction { implicit request =>
      if (isEmployeeOwned(request.identity.emailFilled, request.identity.businessFirst)) {
      val business = BusinessDAO.get(business_id)
      business match {
        case Some(business) =>
          EmployeesBusinessDAO.deleteByEmployeeAndBusiness(employee_id, business_id)
          Home.flashing("success" -> s"Employee $employee_id was assigned")
        case None => Home.flashing("failure" -> s"Business with $business_id not found")
      }
    } else { Home }
}


def toManager(id: Int) = silhouette.SecuredAction { implicit request =>
    if (isEmployeeOwned(request.identity.emailFilled, request.identity.businessFirst)) {
      EmployeeDAO.get(id) match {
        case Some(emp) =>
          Home.flashing(EmployeeDAO.update(id,emp.copy(manager = true)) match {
            case 0 => "failure" -> "Entity has Not been deleted"
            case x => "success" -> {
              s"Entity has been deleted (deleted $x row(s))"
            }
          })
        case _ => Home
      }
    } else { Home }
}
def toParticipator(id: Int) = silhouette.SecuredAction { implicit request =>
    if (isEmployeeOwned(request.identity.emailFilled, request.identity.businessFirst)) {
      EmployeeDAO.get(id) match {
        case Some(emp) =>
          Home.flashing(EmployeeDAO.update(id,emp.copy(manager = false)) match {
            case 0 => "failure" -> "Entity has Not been deleted"
            case x => "success" -> {
              s"Entity has been deleted (deleted $x row(s))"
            }
          })
        case _ => Home
      }
    } else { Home }
}
def destroy(id: Int) = silhouette.SecuredAction { implicit request =>
    if (isEmployeeOwned(request.identity.emailFilled, request.identity.businessFirst)) {
      val emp = EmployeeDAO.get(id)
      val employeeCount = emp match {
        case Some(emp) => EmployeeDAO.getLengthByWorkbench(emp.workbench)
        case _ => 0
      }
      if (employeeCount > 1) { // Max number of employee per workbench are 1
      Home.flashing(EmployeeDAO.delete(id) match {
        case 0 => "failure" -> "Entity has Not been deleted"
        case x => "success" -> {
          val info = models.AccountInfosDAOF.await(models.AccountInfosDAOF.getByInfoByUID(emp.get.uid)) match {
            case Some(info) => {
              if (info.currentWorkbench == Some(emp.get.workbench)) { // Reset current workbench
                models.AccountInfosDAOF.await(models.AccountInfosDAOF.updateCurrentWorkbench(emp.get.uid, None ))
              }
            }
            case _ =>
          }

          s"Entity has been deleted (deleted $x row(s))"
        }
      })
      } else { Home }
    } else { Home }
}


private def isEmployeeOwned(uid: String, business_id: Int):Boolean = {
  EmployeesBusinessDAO.getByUID(uid) match {
    case Some(emp_biz) => {
        EmployeeDAO.getByUID(uid) match {
        case Some(emp) => (emp.manager == true && emp_biz._2 == business_id)
        case _ => false
        }
    }
    case _ => false
  }
}

}
