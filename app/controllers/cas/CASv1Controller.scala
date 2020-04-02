package controllers.cas

import data.CasService
import javax.inject.Inject
import models.{AppsModel, TicketsModel}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class CASv1Controller @Inject()(cc: ControllerComponents, apps: AppsModel, tickets: TicketsModel)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def validate(ticket: String, service: String): Action[AnyContent] = Action.async { implicit rq =>
    apps.getCasApp(service) flatMap {
      case Some(CasService(serviceId, _, _)) =>
        tickets.getCasTicket(ticket, serviceId) map {
          case Some(user) =>
            Ok("yes\n" + user._1.email + "\n")
          case None =>
            Ok("no\n\n")
        }
      case None =>
        Future.successful(BadRequest("no\n\n"))
    }
  }
}
