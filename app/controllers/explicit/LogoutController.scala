package controllers.explicit

import data.App
import javax.inject.Inject
import models.AppsModel
import play.api.Configuration
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class LogoutController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext, apps: AppsModel, config: Configuration) extends AbstractController(cc) {
  def logout(app: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    def redirectUrl: Future[String] = {
      if (app.nonEmpty) {
        apps.getApp(app.get).flatMap {
          case Some(a) => a.redirectUrl + "?logout"
          case _ => "/login"
        }
      } else "/login"
    }

    redirectUrl.map(url => Redirect(url).withNewSession)
  }
}
