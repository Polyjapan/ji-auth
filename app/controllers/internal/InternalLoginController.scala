package controllers.internal

import data.TokensInstance
import javax.inject.Inject
import models.{InternalAppsModel, ServicesModel}
import play.api.mvc._
import play.twirl.api.Html

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
class InternalLoginController @Inject()(cc: ControllerComponents, apps: InternalAppsModel)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def loginGet(service: String): Action[AnyContent] = Action.async { implicit rq =>
    apps.isInternalAppSafe(service).map {
      case Some(safe) =>
        Redirect(controllers.routes.RedirectController.redirectGet())
          .addingToSession(TokensInstance(redirectUrl = service, safe = safe).pair)
      case None => Ok(views.html.errorPage("Service interne introuvable", Html("<p>Le service spécifié est introuvable. Merci de signaler cette erreur au créateur du site dont vous provenez.")));
    }
  }
}
