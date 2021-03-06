package controllers
import utils.auth.DefaultEnv
import models.DAO.resources.{BusinessDAO, BusinessDTO}
import models.DAO._
import play.api._
import play.api.mvc._
import play.twirl.api.Html
import play.api.http.MimeTypes
import play.api.libs.json._
import play.api.cache._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats
import play.api.data.format.Formatter
import play.api.data.FormError
import play.api.Logger
import views._
import models.User
import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import forms._
import models.User2
import play.api.i18n.{ I18nSupport, MessagesApi }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import models.DAO.BProcessDTO
import models.DAO.BPDAO
import models.DAO._
import models.DAO.resources._
import models.DAO.CompositeValues
import play.api.Play.current
import main.scala.bprocesses._
import main.scala.simple_parts.process._
import models.DAO.reflect._
import models.DAO.conversion._
import ProcHistoryDAO._
import helpers._
import decorators._
import builders._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Inject
import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import forms._
import models.User2
import play.api.i18n.{ I18nSupport, MessagesApi }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import us.ority.min.actions._
import models.DAO.reflect._
import main.scala.bprocesses.refs._
import main.scala.bprocesses.refs.{BPStateRef}
import models.DAO.conversion._
import play.api.mvc.{ Action, RequestHeader }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Awaitable, Await, Future}
import scala.util.Try
import utils.auth.DefaultEnv

import java.util.UUID
import javax.inject.Inject
import play.api.mvc.Controller

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import forms.SignUpForm
import models.User2
import models.services.UserService
import play.api.i18n._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Action
import com.nappin.play.recaptcha.{RecaptchaVerifier, WidgetHelper}
import scala.concurrent.Future
import models.daos._
import com.mohiva.play.silhouette.api.util.{ PasswordHasher, PasswordInfo }



class UtilitiesController @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
  socialProviderRegistry: SocialProviderRegistry,
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  avatarService: AvatarService,
  val verifier: RecaptchaVerifier,
  passwordHasher: PasswordHasher)(
  implicit widgetHelper: WidgetHelper)
    extends Controller with I18nSupport {

    // Update refs for element identificator
    // Given ref elememt identificator
    def updateRefByIdent(elType: String, typeTitle: String) = {
      def await[T](a: Awaitable[T])(implicit ec: ExecutionContext) = Await.result(a, Duration.Inf)
      // find elements with the ident
      val elements = ProcElemDAO.findByTypeAndTypeTitle(elType, typeTitle)
      val newRef_elementOpt = ProcElemReflectionDAOF.findByTypeAndTypeTitle(elType, typeTitle)
      newRef_elementOpt match {
        case Some(newRef_element) => {
          val topoId = await(ReflectElemTopologDAOF.findByFrontId(newRef_element.id.get))get
          val actionsRef = await(ReactionRefDAOF.findAllByElem(topoId.id.get))
          /*********************************************************************************
           find action ref and prepare action payload
           action payloads takes place only if there is middleware that existing
          */
          val refActionContainerList:List[RefActionContainer] = actionsRef.map { actionRef =>
            val middlewareOpt = await(MiddlewareRefsDAOF.getAllByReaction(actionRef.id.get) ).headOption
            middlewareOpt match {
              case Some(middleware) => {
                val strategyOpt = await(StrategyRefsDAOF.getByMWS(List(middleware.id.get))).headOption
                strategyOpt match {
                  case Some(strategy) => {
                    val inputs = await(StrategyInputRefsDAOF.getByStrategy(strategy.id.get) )
                    val bases = await(StrategyBaseRefsDAOF.getByStrategies(List(strategy.id.get)) )
                    val outputs = await(StrategyOutputRefsDAOF.getByStrategy(strategy.id.get) )
                       Option(RefActionContainer(
                              action_id = actionRef.id.get,
                              middleware_id = middleware.id.get,
                              strategy_id = strategy.id.get,
                              bases = bases.map { base =>
                                BaseContainer(
                                  base_id = base.id.get,
                                  base_req_type = "string",
                                  base_content_string = "",
                                  base_content_number = 0L,
                                  base_content_boolean = false
                                )
                              }.toList
                        ) )
                  }
                  case _ => None
                }
              }
              case _ => None
            }
          }.flatten.toList

          // old elements iterator
          elements.map { element =>
            ProcElemDAO.deleteWithoutOrderRenew(element.id.get)
            RefDAOF.retrieveAndCreateElementWithOrder(
                          refId = newRef_element.reflection,
                          process = element.bprocess,
                          order = element.order,
                          business = element.business,
                          in = "front",
                          title = element.title,
                          desc = element.desc,
                          space_id = None,
                          refActionContainer = refActionContainerList
            )

          }
        }
        case _ => "Ref not found"
      }
}

def replaceStrategy() = {
  // there is element
  // there is RefActionContainer
  // need ref element id
  // need ref element action id
  // partial projection possible
}

// Copy users
def copyUser(user1: String, user2: String) = silhouette.SecuredAction.async { implicit request =>
  // copy creds
  val findedUser1 = com.mohiva.play.silhouette.api.LoginInfo("credentials", user1)
  val findedUser2 = com.mohiva.play.silhouette.api.LoginInfo("credentials", user2)

  val user1PasswordF: Future[Option[DBPasswordInfo]] = userService.findPasswordHash(findedUser1)

  user1PasswordF.flatMap { passOpt =>
    passOpt match {
      case Some(pass) => {
        userService.retrieve(findedUser2).flatMap {
          case Some(user) =>
            Future.successful(  Ok(Json.toJson(  Map("error" -> "You are already registered") )  ) )
          case None =>
            val authInfo = PasswordInfo(
                            hasher = "bcrypt",
                            password = pass.password
                          ) //passwordHasher.hash(pass.password)
            val loginInfo1 = LoginInfo(CredentialsProvider.ID, user2)

            val user = User2(
              userID = UUID.randomUUID(),
              loginInfo = loginInfo1,
              firstName = Some("First name"),
              lastName = Some("Last name"),
              fullName = Some("Full name"),
              email = Some(user2),
              avatarURL = None
            )

            mailers.Mailer.sendInvite(subject = "Welcome | Minority Platform",
                     emails = List(user2),
                     invite_link = "")
            for {
              avatar <- avatarService.retrieveURL(user2)
              user <- userService.save(user.copy(avatarURL = avatar))
              authInfo <- authInfoRepository.add(loginInfo1, authInfo)
              //authenticator <- silhouette.env.authenticatorService.create(loginInfo)
              //value <- silhouette.env.authenticatorService.init(authenticator)
            } yield {
              //silhouette.env.eventBus.publish(SignUpEvent(user, request))
              Ok( Json.toJson( Map("success" -> s"${user1} was copied", "target" -> s"${user2}")  ) )
            }
        }
      }
      case _ =>  Future.successful( Ok( Json.toJson( Map("error" -> "User not found")  )) )
    }
  }

}







}
