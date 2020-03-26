package controllers.api

import java.util.{Date, UUID}

import ch.japanimpact.auth.api
import ch.japanimpact.auth.api.AppTicketResponse
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import javax.inject.Inject
import models.{AppsModel, GroupsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import services.JWTService
import utils.Implicits._
import utils.RandomUtils

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class AppTicketController @Inject()(cc: ControllerComponents,
                                    tickets: TicketsModel,
                                    groups: GroupsModel,
                                    users: UsersModel

                                   )(implicit ec: ExecutionContext, apps: AppsModel, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {

  def getAppTicket(ticket: String): Action[AnyContent] = Action.async { implicit rq =>
    ApiUtils.withApp { app =>
      tickets useTicket(ticket, app) flatMap {
        case Some((data.Ticket(_, _, _, validTo, ticketType), data.RegisteredUser(Some(id), email, _, _, _, _, _, _, _, _, _))) =>
          // Found some ticket bound to the app
          if (validTo.before(new Date)) !InvalidTicket // The ticket is no longer valid
          else {
            groups.getGroups(app, id).flatMap(groupSet => {
              users.getUserProfile(id).map(profile => {
                val (details, address) = profile match {
                  case Some((user, address)) => (user.toUserDetails, address.map(_.toUserAddress))
                }

                val userProfile = api.UserProfile(id, email, details, address)

                val resp = AppTicketResponse(id, email, ticketType, groupSet, userProfile)
                resp
              })
            })
          } // The ticket is valid, return the data
        case None => !InvalidTicket
      }
    }
  }

}
