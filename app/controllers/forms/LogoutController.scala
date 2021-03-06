package controllers.forms

import data.CasService
import data.UserSession._
import javax.inject.Inject
import models.{ServicesModel, SessionsModel, TicketsModel}
import play.api.Configuration
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.twirl.api.Html
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class LogoutController @Inject()(cc: ControllerComponents)
                                (implicit ec: ExecutionContext, cas: ServicesModel,
                                 config: Configuration, sessions: SessionsModel) extends AbstractController(cc) {

  def logout(app: Option[String], redirect: Option[String], service: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    def redirectUrl: Future[String] = {
      if (app.orElse(service).nonEmpty) {
        cas.getCasService(app.orElse(service).get).map {
          case Some(CasService(_, _, Some(url), _, _, _, _, _, _)) if url.trim.nonEmpty => url + "?logout"
          case Some(_) => app.orElse(service).get + "?logout"
          case None => "/login"
        }
      } else "/login"
    }

    if (rq.hasUserSession) {
      sessions.logoutSession(rq.userSession.sessionKey)
        .flatMap(_ => redirectUrl)
        .map(url => Redirect(url).withNewSession)
    } else {
      Unauthorized(views.html.errorPage("Non connecté", Html("<p>Impossible de vous déconnecter : vous n'êtes pas connecté.</p>")))
    }
  }
}
