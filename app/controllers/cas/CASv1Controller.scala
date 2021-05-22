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
      case Some(s: CasService) =>
        tickets.getCasTicket(ticket, s.serviceId.get) map {
          case Some(((user, _), _)) =>
            Ok("yes\n" + user.id.get + "\n")
          case None =>
            Ok("no\n\n")
        }
      case None =>
        tickets.invalidateCasTicket(ticket).map(_ => BadRequest("no\n\n"))
    }
  }
}
