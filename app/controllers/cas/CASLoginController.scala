package controllers.cas

import data.{AuthenticationInstance, CASInstance}
import javax.inject.Inject
import models.ServicesModel
import play.api.mvc._
import play.twirl.api.Html

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
class CASLoginController @Inject()(cc: ControllerComponents, apps: ServicesModel)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def loginGet(service: String): Action[AnyContent] = Action.async { implicit rq =>
    apps.getCasService(service).map {
      case Some(casService) =>
        Redirect(controllers.forms.routes.RedirectController.redirectGet())
          .addingToSession(CASInstance(url = casService.serviceRedirectUrl.getOrElse(service), casService.serviceId).pair)
      case None => Ok(views.html.errorPage("Service introuvable", Html("<p>Le service spécifié est introuvable. Merci de signaler cette erreur au créateur du site dont vous provenez.")));
    }
  }
}
