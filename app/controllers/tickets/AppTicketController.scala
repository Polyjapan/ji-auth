package controllers.tickets

import ch.japanimpact.auth.api
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import ch.japanimpact.auth.api.{AppTicketResponse, TicketType}
import data.CasService
import javax.inject.Inject
import models.{ServicesModel, GroupsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class AppTicketController @Inject()(cc: ControllerComponents,
                                    tickets: TicketsModel,
                                    groups: GroupsModel,
                                    users: UsersModel

                                   )(implicit ec: ExecutionContext, apps: ServicesModel, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {


  private def withApp(body: CasService => Future[Result])(implicit apps: ServicesModel, request: RequestHeader, ec: ExecutionContext): Future[Result] = {
    val h = request.headers

    if (!h.hasHeader("X-Client-Id")) {
      Results.Unauthorized(Json.toJson[RequestError](MissingData))
    } else {
      apps.getCasService(h("X-Client-Id")) flatMap {
        case Some(app) => body(app)
        case None => !InvalidAppSecret
      }
    }
  }

  // This is a deprecated method
  // This is an adapter to enable CAS-like features for classic apps
  def getAppTicket(ticket: String): Action[AnyContent] = Action.async { implicit rq =>
    withApp { app =>
      tickets.getCasTicket(ticket, app.serviceId) flatMap {
        case Some((user, groups)) =>

          users.getUserProfile(user.id.get).map(profile => {
            val (details, address) = profile match {
              case Some((user, address)) => (user.toUserDetails, address.map(_.toUserAddress))
            }

            val userProfile = api.UserProfile(user.id.get, user.email, details, address)
            AppTicketResponse(user.id.get, user.email, TicketType.ExplicitGrantTicket, groups, userProfile)
          })

        case None => !InvalidTicket
      }
    }
  }


}
