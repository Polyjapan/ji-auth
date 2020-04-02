package controllers.forms

import data.CasService
import javax.inject.Inject
import models.{InternalAppsModel, ServicesModel}
import play.api.Configuration
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class LogoutController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext, cas: ServicesModel, internal: InternalAppsModel, config: Configuration) extends AbstractController(cc) {
  def logout(app: Option[String], redirect: Option[String], service: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    def redirectUrl: Future[String] = {
      if (app.orElse(service).nonEmpty) {
        cas.getCasService(app.orElse(service).get).map {
          case Some(CasService(_, _, Some(url))) => url + "?logout"
          case Some(_) => app.get + "?logout"
          case None => "/login"
        }
      } else if (redirect.nonEmpty) {
        internal.isInternalApp(redirect.get).map {
          case true => redirect.get + "?logout"
          case false => "/login"
        }
      } else "/login"
    }

    redirectUrl.map(url => Redirect(url).withNewSession)
  }
}
