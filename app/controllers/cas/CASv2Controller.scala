package controllers.cas

import data.CasService
import javax.inject.Inject
import models.{AppsModel, TicketsModel}
import play.api.mvc._
import utils.CAS

import scala.concurrent.{ExecutionContext, Future}
import utils.Implicits._

/**
 * @author Louis Vialar
 */
class CASv2Controller @Inject()(cc: ControllerComponents, apps: AppsModel, tickets: TicketsModel)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def proxyValidate(ticket: String, service: String): Action[AnyContent] = serviceValidate(ticket, service) // TODO

  def serviceValidate(ticket: String, service: String): Action[AnyContent] = Action.async { implicit rq =>
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
