package controllers.explicit

import ch.japanimpact.auth.api.TicketType
import data.App
import data.UserSession._
import models.{AppsModel, TicketsModel}
import play.api.mvc.{RequestHeader, Result, Results}
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
object ExplicitTools {
  private[explicit] def produceRedirectUrl(app: Option[String], userId: Int)(implicit apps: AppsModel, tickets: TicketsModel, ec: ExecutionContext): Future[String] = {
    if (app.nonEmpty) {
      apps.getApp(app.get).flatMap {
        case Some(App(Some(appId), _, _, _, _, redirectUrl, _, _)) =>
          tickets.createTicketForUser(userId, appId, TicketType.ExplicitGrantTicket).map(ticket => redirectUrl + "?ticket=" + ticket)
        case _ => "/"
      }
    } else "/"
  }

  private[explicit] def checkLoggedIn(app: Option[String])(implicit apps: AppsModel, tickets: TicketsModel, ec: ExecutionContext, request: RequestHeader): Option[Future[String]] = {
    if (request.hasUserSession) {
      Some(
        if (app.isEmpty) Future("/")
        else {
          apps.getApp(app.get).flatMap {
            case Some(App(Some(appId), _, _, _, _, redirectUrl, _, _)) =>
              tickets.createTicketForUser(request.userSession.id, appId, TicketType.ExplicitGrantTicket).map(ticket => redirectUrl + "?ticket=" + ticket)
            case _ => "/"
          }
        }
      )
    } else None
  }

  private[explicit] def ifLoggedOut(app: Option[String])(body: => Future[Result])(implicit apps: AppsModel, tickets: TicketsModel, ec: ExecutionContext, request: RequestHeader): Future[Result] = {
    val redirect = ExplicitTools.checkLoggedIn(app)
    if (redirect.nonEmpty) {
      redirect.get.map(url => Results.Redirect(url))
    } else body
  }
}
