package controllers.api

import java.util.Date

import ch.japanimpact.auth.api.AuthApi.{AppTicketRequest, AppTicketResponse}
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import javax.inject.Inject
import models.{AppsModel, GroupsModel, TicketsModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class AppTicketController @Inject()(cc: ControllerComponents,
                                    tickets: TicketsModel,
                                    groups: GroupsModel)(implicit ec: ExecutionContext, apps: AppsModel, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {


  @deprecated
  def postAppTicket: Action[AppTicketRequest] = Action.async(parse.json[AppTicketRequest]) { implicit rq =>
    if (rq.hasBody) {
      val body = rq.body

      ApiUtils.withGivenApp(body.clientId, body.clientSecret) { app =>
        returnTicket(app, body.ticket)
      }
    }
    else !MissingData // No body or body parse fail ==> invalid input
  }

  private def returnTicket(app: data.App, ticket: String): Future[Result] = {
    tickets useTicket(ticket, app) flatMap {
      case Some((data.Ticket(_, _, _, validTo, ticketType), data.RegisteredUser(Some(id), email, _, _, _, _, _, _))) =>
        // Found some ticket bound to the app
        if (validTo.before(new Date)) !InvalidTicket // The ticket is no longer valid
        else {
          groups getGroups(app, id) map (groupSet => AppTicketResponse(id, email, ticketType, groupSet))
        } // The ticket is valid, return the data
      case None => !InvalidTicket
    }
  }

  def getAppTicket(ticket: String): Action[AnyContent] = Action.async { implicit rq =>
    ApiUtils.withApp { app =>
      returnTicket(app, ticket)
    }
  }

}
