package controllers.forms

import java.net.URLDecoder

import data.UserSession
import javax.inject.Inject
import models.{SessionsModel, UsersModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import utils.Implicits._

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
class EmailConfirmController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext, mailer: MailerClient, sessions: SessionsModel, config: Configuration, users: UsersModel) extends AbstractController(cc) {

  def emailConfirmGet(email: String, code: String): Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedOut {
      val decode = (s: String) => URLDecoder.decode(s, "UTF-8")

      users.confirmEmail(decode(email), decode(code)).flatMap {
        case Some(user) =>
          sessions.createSession(user.id.get, rq.remoteAddress, rq.headers.get("User-Agent").getOrElse("unknown"))
            .map(sid => Ok(views.html.register.emailconfirmok()).addingToSession(UserSession(user, sid): _*))
        case None =>
          BadRequest(views.html.register.emailconfirmfail())
      }
    }
  }
}
