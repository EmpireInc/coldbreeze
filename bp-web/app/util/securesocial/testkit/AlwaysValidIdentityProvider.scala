package securesocial.testkit

import play.api.Logger
import play.api.mvc.Request
import securesocial.core.AuthenticationResult.Authenticated
import securesocial.core._

import scala.collection.immutable.ListMap
import scala.concurrent.Future
import models._

class AlwaysValidIdentityProvider extends IdentityProvider{
  val logger = Logger("securesocial.stubs.AlwaysValidIdentityProvider")
  def authMethod: AuthenticationMethod = AuthenticationMethod("naive")

  override def authenticate()(implicit request: Request[play.api.mvc.AnyContent]): Future[AuthenticationResult] = {
  	val profile = BasicProfileGenerator.basicProfile()
  	models.AccountsDAO.save(profile)
    Future.successful(Authenticated(profile))
  }

  val id: String = "naive"
}
object AlwaysValidIdentityProvider{

  abstract class RuntimeEnvironment[U] extends RuntimeEnvironment.Default[U]{
    override lazy val providers: ListMap[String, IdentityProvider] = ListMap(include(new AlwaysValidIdentityProvider))
  }
}
