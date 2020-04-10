package controllers.cas

import data.CasService
import javax.inject.Inject
import models.{ServicesModel, TicketsModel}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class CASv1Controller @Inject()(cc: ControllerComponents, apps: ServicesModel, tickets: TicketsModel)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def validate(ticket: String, service: String): Action[AnyContent] = Action.async { implicit rq =>
    apps.getCasService(service) flatMap {
      case Some(CasService(serviceId, _, _, _)) =>
        tickets.getCasTicket(ticket, serviceId.get) map {
          case Some(user) =>
            Ok("yes\n" + user._1.id.get + "\n")
          case None =>
            Ok("no\n\n")
        }
      case None =>
        tickets.invalidateCasTicket(ticket).map(_ => BadRequest("no\n\n"))
    }
  }
}
