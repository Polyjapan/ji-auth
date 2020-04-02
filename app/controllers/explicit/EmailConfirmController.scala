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
class EmailConfirmController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration, users: UsersModel) extends AbstractController(cc) {

  def emailConfirmGet(email: String, code: String): Action[AnyContent] = Action.async { implicit rq =>
    ExplicitTools.ifLoggedOut {
      val decode = (s: String) => URLDecoder.decode(s, "UTF-8")

      users.confirmEmail(decode(email), decode(code)).flatMap {
        case Some(user) =>
          Ok(views.html.register.emailconfirmok()).addingToSession(UserSession(user): _*)
        case None =>
          BadRequest(views.html.register.emailconfirmfail())
      }
    }
  }
}
