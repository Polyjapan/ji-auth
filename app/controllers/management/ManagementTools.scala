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
    else Future(Results.Redirect(controllers.explicit.routes.LoginController.loginGet(None)))
  }

  private[management] def ifPermission(permission: Int)(body: UserSession => Future[Result])(implicit ec: ExecutionContext, request: RequestHeader): Future[Result] = {
    ifLoggedIn(implicit session => {
      if (session.hasPermission(permission)) body(request.userSession)
      else Future(Results.Forbidden(error("Permissions manquantes", "Vous n'avez pas le droit de faire cela.")))
    })
  }

  private[management] def error(title: String, message: String)(implicit userSession: UserSession) = {
    views.html.management.managementError(userSession, title, message)
  }
}
