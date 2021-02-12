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

          // Note regarding TFA: when the user confirms their email, they can't possibly have set-up TFA since it's not
          // possible to login while email is not confirmed.
          // ==> BECAUSE OF THIS (and ONLY because of this) we can allow a direct login after email confirmation
          // Also, we still need to make sure the confirm key is unique and random enough so that's it's "impossible" to
          // guess for an attacker while the account hasn't been confirmed :)

          sessions.createSession(user.id.get, rq.remoteAddress, rq.headers.get("User-Agent").getOrElse("unknown"))
            .map(sid => Ok(views.html.register.emailconfirmok()).addingToSession(UserSession(user, sid): _*))
        case None =>
          BadRequest(views.html.register.emailconfirmfail())
      }
    }
  }
}
