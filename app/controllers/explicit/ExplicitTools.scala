package controllers.explicit

import ch.japanimpact.auth.api.{LoginSuccess, TicketType, TokenResponse}
import data.{App, CasService, RegisteredUser, UserSession}
import data.UserSession._
import javax.inject.Inject
import models.{AppsModel, GroupsModel, SessionsModel, TicketsModel, UsersModel}
import play.api.mvc.{RequestHeader, Result, Results}
import services.JWTService
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class ExplicitTools @Inject()(apps: AppsModel, tickets: TicketsModel, groups: GroupsModel, users: UsersModel, sessions: SessionsModel, jwt: JWTService)(implicit ec: ExecutionContext) {

  private[explicit] def produceRedirectUrl(app: Option[String], tokenType: Option[String], userId: Int): Future[String] = {
    if (app.nonEmpty) {
      if (tokenType.exists(_.startsWith("cas"))) {
        apps.getCasApp(app.get).flatMap {
          case Some(CasService(serviceId, _)) =>
            apps.hasRequiredGroups(serviceId, userId).flatMap(hasRequired => {
              val url = app.get
              val symbol = if (url.contains("?")) "&" else "?"
              if (hasRequired) tickets.createCasTicketForUser(userId, serviceId).map(ticket => url + symbol + "ticket=" + ticket)
              else "/"
            })
          case None => "/"
        }
      } else {
        apps.getApp(app.get).flatMap {
          case Some(App(Some(appId), _, _, _, _, redirectUrl, _, _)) =>
            tokenType.getOrElse("ticket") match {
              case "ticket" =>
                tickets.createTicketForUser(userId, appId, TicketType.ExplicitGrantTicket).map(ticket => redirectUrl + "?ticket=" + ticket)
              case "token" =>
                groups
                  .getGroupsByMember(userId)
                  .flatMap(groups => sessions.createSession(userId).map(sessionId => (sessionId, groups)))
                  .map {
                    case (sid, groups) =>
                      val token = jwt.issueToken(userId, groups.map(_.name).toSet)
                      val refresh = sid.toString

                      redirectUrl + "?accessToken=" + token + "&refreshToken=" + refresh + "&duration=" + (jwt.ExpirationTimeMinutes * 60)
                  }
            }
          case _ => "/"
        }
      }
    } else "/"
  }

  private[explicit] def produceRedirectOrCompleteInfo(app: Option[String], tokenType: Option[String], userId: Int): Future[String] = {
      if (app.isEmpty) Future("/")
      else {
        users.getUserProfile(userId).flatMap {
          case Some((RegisteredUser(_, _, _, _, _, _, _, _, _, _, Some(_)), Some(_))) => // Okay

            produceRedirectUrl(app, tokenType, userId)
          case Some(_) =>
            Future(routes.UpdateInfoController.updateGet(app, tokenType).url)
        }

      }
  }

  private[explicit] def checkLoggedInAndUpToDate(app: Option[String], tokenType: Option[String])(implicit request: RequestHeader): Option[Future[String]] = {
    if (request.hasUserSession) Some(produceRedirectOrCompleteInfo(app, tokenType, request.userSession.id))
    else None
  }

  private[explicit] def ifLoggedOut(app: Option[String], tokenType: Option[String])(body: => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    val redirect = checkLoggedInAndUpToDate(app, tokenType)
    if (redirect.nonEmpty) {
      redirect.get.map(url => Results.Redirect(url))
    } else body
  }

  private[explicit] def ifLoggedIn(app: Option[String], tokenType: Option[String])(body: UserSession => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    if (request.hasUserSession) {
      body(request.userSession)
    } else {
      Results.Redirect(routes.LoginController.loginGet(app, tokenType).url)
    }
  }


}
