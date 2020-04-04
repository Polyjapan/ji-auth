package controllers.internal

import ch.japanimpact.auth.api.TokenResponse
import data._
import javax.inject.Inject
import models.{ServicesModel, SessionsModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import services.JWTService
import utils.Implicits._

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class TokenRefreshController @Inject()(cc: ControllerComponents,
                                       sessions: SessionsModel, jwt: JWTService

                                   )(implicit ec: ExecutionContext, apps: ServicesModel, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {

  def refreshToken(refreshToken: String): Action[AnyContent] = Action.async { implicit rq =>
    try {
      val sesId = SessionID(refreshToken)

      sessions.getSession(sesId).map {
        case Some((uid, groups)) =>
          val newToken = jwt.issueInternalToken(uid, groups)

          TokenResponse(newToken, refreshToken, jwt.ExpirationTimeMinutes * 60)
        case None =>
          Unauthorized
      }
    } catch {
      case e: IllegalArgumentException => BadRequest
    }
  }

}
