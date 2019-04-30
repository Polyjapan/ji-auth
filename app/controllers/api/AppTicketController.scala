package controllers.api

import java.util.Date

import ch.japanimpact.auth.api.AuthApi.{AppTicketRequest, AppTicketResponse}
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import javax.inject.Inject
import models.{AppsModel, TicketsModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import utils.Implicits._

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class AppTicketController @Inject()(cc: ControllerComponents,
                                    tickets: TicketsModel,
                                    apps: AppsModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {


  /**
    * The ticket has expired, is invalid, or is not readable by the current client
    */

  def postAppTicket: Action[AppTicketRequest] = Action.async(parse.json[AppTicketRequest]) { implicit rq =>
    if (rq.hasBody) {
      val body = rq.body

      // Get client first
      apps getAuthentifiedApp(body.clientId, body.clientSecret) flatMap {
        case Some(app) =>
          // Found some app (clientId and clientSecret are therefore valid)
          tickets useTicket(body.ticket, app) map {
            case Some((data.Ticket(_, _, _, validTo, ticketType), data.RegisteredUser(Some(id), email, _, _, _, _, _))) =>
              // Found some ticket bound to the app
              if (validTo.before(new Date)) !InvalidTicket // The ticket is no longer valid
              else AppTicketResponse(id, email, ticketType) // The ticket is valid, return the data
            case None => !InvalidTicket
          }

        case None => !InvalidAppSecret
      }
    } else !MissingData // No body or body parse fail ==> invalid input
  }


}
