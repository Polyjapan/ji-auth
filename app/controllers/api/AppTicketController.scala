package controllers.api

import java.util.Date

import ch.japanimpact.auth.api.AppTicketResponse
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import javax.inject.Inject
import models.{AppsModel, GroupsModel, TicketsModel}
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import services.JwtService
import utils.Implicits._

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class AppTicketController @Inject()(cc: ControllerComponents,
                                    tickets: TicketsModel,
                                    groups: GroupsModel,
                                    jwt: JwtService

                                   )(implicit ec: ExecutionContext, apps: AppsModel, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {

  def getAppTicket(ticket: String): Action[AnyContent] = Action.async { implicit rq =>
    ApiUtils.withApp { app =>
      tickets useTicket(ticket, app) flatMap {
        case Some((data.Ticket(_, _, _, validTo, ticketType), data.RegisteredUser(Some(id), email, _, _, _, _, _, _, _, _, _))) =>
          // Found some ticket bound to the app
          if (validTo.before(new Date)) !InvalidTicket // The ticket is no longer valid
          else {
            groups getGroups(app, id) map (groupSet => {
              val resp = AppTicketResponse(id, email, ticketType, groupSet)
              val claim = Json.toJson(resp).as[JsObject]

              println("Nice jwt:")
              println(jwt.encodeJson(claim))

              resp
            })
          } // The ticket is valid, return the data
        case None => !InvalidTicket
      }
    }
  }

}
