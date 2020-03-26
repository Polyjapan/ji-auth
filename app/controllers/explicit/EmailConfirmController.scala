package controllers.explicit

import java.net.URLDecoder

import data.UserSession._
import data.{RegisteredUser, UserSession}
import javax.inject.Inject
import models.{AppsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import utils.Implicits._

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class EmailConfirmController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration, ExplicitTools: ExplicitTools, users: UsersModel) extends AbstractController(cc) {

  def emailConfirmGet(email: String, code: String, app: Option[String], tokenType: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    ExplicitTools.ifLoggedOut(app, tokenType) {
      val decode = (s: String) => URLDecoder.decode(s, "UTF-8")

      def ok(user: RegisteredUser, redirect: String): Result =
        Ok(views.html.register.emailconfirmok(redirect)).withSession(UserSession(user): _*)

      users.confirmEmail(decode(email), decode(code)).flatMap {
        case Some(user) =>
          ExplicitTools.produceRedirectOrCompleteInfo(app, tokenType, user.id.get).map(url => ok(user, url))
        case None =>
          BadRequest(views.html.register.emailconfirmfail())
      }
    }
  }
}
