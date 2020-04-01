package controllers.api

import java.util.{Date, UUID}

import ch.japanimpact.auth.api
import ch.japanimpact.auth.api.AppTicketResponse
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import data.CasService
import javax.inject.Inject
import models.{AppsModel, GroupsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import services.JWTService
import utils.Implicits._
import utils.{CAS, RandomUtils}

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

  def getCasV1Ticket(ticket: String, service: String): Action[AnyContent] = Action.async { implicit rq =>
    apps.getCasApp(service) flatMap {
      case Some(CasService(serviceId, _)) =>
        tickets.getCasTicket(ticket, serviceId) map {
          case Some(user) =>
            Ok("yes\n" + user._1.email + "\n")
          case None =>
            Ok("no\n\n")
        }
      case None =>
        BadRequest("no\n\n")
    }
  }

  def getCasV2Ticket(ticket: String, service: String): Action[AnyContent] = Action.async { implicit rq =>
    apps.getCasApp(service) flatMap {
      case Some(CasService(serviceId, _)) =>
        println(" Service " + serviceId + " found")
        tickets.getCasTicket(ticket, serviceId) flatMap {
          case Some((user, groups)) =>
            val params = Map(
              "email" -> user.email,
              "name" -> (user.firstName + " " + user.lastName),
              "firstname" -> user.firstName,
              "lastname" -> user.lastName
            )

            Ok(CAS.getCasSuccessMessage(params, user.email, groups))
          case None =>
            Ok(CAS.getCasErrorResponse(CAS.CASError.InvalidTicket, ticket))
        }
      case None =>
        Ok(CAS.getCasErrorResponse(CAS.CASError.InvalidService, CAS.getServiceDomain(service).getOrElse(service)))
    }
  }

}
