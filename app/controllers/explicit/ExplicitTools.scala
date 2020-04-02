package controllers.explicit

import data.UserSession
import data.UserSession._
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.Future

/**
 * @author Louis Vialar
 */
object ExplicitTools {

  def ifLoggedOut(body: => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    if (request.hasUserSession) {
      Future.successful(Results.Redirect(controllers.forms.routes.RedirectController.redirectGet()))
    } else body
  }

  def ifLoggedIn(body: UserSession => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    if (request.hasUserSession) {
      body(request.userSession)
    } else {
      Future.successful(Results.Redirect(routes.LoginController.loginGet(None, None).url))
    }
  }


}
