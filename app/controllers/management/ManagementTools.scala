package controllers.management

import data.UserSession
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import data.UserSession._

/**
  * @author Louis Vialar
  */
object ManagementTools {
  private[management] def ifLoggedIn(body: UserSession => Future[Result])(implicit ec: ExecutionContext, request: RequestHeader): Future[Result] = {
    if (request.hasUserSession) body(request.userSession)
    else Future(Results.Redirect(controllers.forms.routes.LoginController.loginGet(None, None)))
  }

  private[management] def error(title: String, message: String)(implicit userSession: UserSession) = {
    views.html.management.managementError(userSession, title, message)
  }
}
