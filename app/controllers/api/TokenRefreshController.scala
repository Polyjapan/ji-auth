package controllers.api

import java.util.Date

import ch.japanimpact.auth.api
import ch.japanimpact.auth.api.{AppTicketResponse, TokenResponse}
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import javax.inject.Inject
import models.{AppsModel, GroupsModel, SessionsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import services.JWTService
import utils.Implicits._

import scala.concurrent.ExecutionContext
import data._

/**
  * @author Louis Vialar
  */
class TokenRefreshController @Inject()(cc: ControllerComponents,
                                       sessions: SessionsModel, jwt: JWTService

                                   )(implicit ec: ExecutionContext, apps: AppsModel, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {

  def refreshToken(refreshToken: String): Action[AnyContent] = Action.async { implicit rq =>
    try {
      val sesId = SessionID(refreshToken)

      sessions.getSession(sesId).map {
        case Some((uid, groups)) =>
          val newToken = jwt.issueToken(uid, groups)

          TokenResponse(newToken, refreshToken, jwt.ExpirationTimeMinutes * 60)
        case None =>
          Unauthorized
      }
    } catch {
      case e: IllegalArgumentException => BadRequest
    }
  }

}
