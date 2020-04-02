package controllers.internal

import data.TokensInstance
import javax.inject.Inject
import models.AppsModel
import play.api.mvc._
import play.twirl.api.Html

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
class InternalLoginController @Inject()(cc: ControllerComponents, apps: AppsModel)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def loginGet(service: String): Action[AnyContent] = Action.async { implicit rq =>
    apps.isInternalApp(service).map {
      case true =>
        Redirect(controllers.forms.routes.RedirectController.redirectGet())
          .addingToSession(TokensInstance(redirectUrl = service).pair)
      case false => Ok(views.html.errorPage("Service interne introuvable", Html("<p>Le service spécifié est introuvable. Merci de signaler cette erreur au créateur du site dont vous provenez.")));
    }
  }
}
