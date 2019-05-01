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
}
