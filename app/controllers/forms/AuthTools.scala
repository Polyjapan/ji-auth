package controllers.forms

import controllers.forms.routes
import data.UserSession
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.Future
import UserSession._

/**
 * @author Louis Vialar
 */
object AuthTools {

  def ifLoggedOut(body: => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    if (request.hasUserSession) {
      Future.successful(Results.Redirect(controllers.routes.RedirectController.redirectGet()))
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
