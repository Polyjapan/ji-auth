package controllers.cas

import data.{AuthenticationInstance, CASInstance}
import javax.inject.Inject
import models.ServicesModel
import play.api.mvc._
import play.twirl.api.Html

import scala.concurrent.ExecutionContext
import data.UserSession._

/**
 * @author Louis Vialar
 */
class CASLoginController @Inject()(cc: ControllerComponents, apps: ServicesModel)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def loginGet(service: String, renew: Option[Boolean], gateway: Option[Boolean]): Action[AnyContent] = Action.async { implicit rq =>
    apps.getCasService(service).map {
      case Some(casService) =>
        if (gateway.getOrElse(false) && !rq.hasUserSession) {
          // Gateway: if param is set, we should not attempt to log the user in
          Redirect(casService.serviceRedirectUrl.getOrElse(service))
        } else {
          val ret = Redirect(controllers.routes.RedirectController.redirectGet())
          val instance = CASInstance(url = casService.serviceRedirectUrl.getOrElse(service), casService.serviceId.get, requireFullInfo = casService.serviceRequiresFullInfo)

          if (renew.getOrElse(false))
            // Renew: if param is set, we should drop the existing user session and ask the user to log in again
            ret.withSession(instance.pair)
          else
            ret.addingToSession(instance.pair)
        }
      case None => Ok(views.html.errorPage("Service introuvable", Html("<p>Le service spécifié est introuvable. Merci de signaler cette erreur au créateur du site dont vous provenez.")));
    }
  }
}
