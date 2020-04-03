package controllers

import data.UserSession._
import data.{Address, AuthenticationInstance, CASInstance, RegisteredUser, TokensInstance}
import javax.inject.Inject
import models._
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import services.JWTService
import utils.Implicits._
import utils.RandomUtils

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class RedirectController @Inject()(cc: MessagesControllerComponents)
                                  (implicit ec: ExecutionContext, config: Configuration,
                                   users: UsersModel, tickets: TicketsModel, groups: GroupsModel,
                                   sessions: SessionsModel, jwt: JWTService,
                                   apps: ServicesModel) extends MessagesAbstractController(cc) with I18nSupport {


  private def produceRedirection(user: RegisteredUser, address: Address, authInstance: Option[AuthenticationInstance])(implicit rq: RequestHeader): Future[Result] = {
    val userId = user.id.get

    (if (authInstance.isEmpty) Redirect("/").asFuture
    else authInstance.get match {
      case CASInstance(url, serviceId) =>
        apps.hasRequiredGroups(serviceId, userId).flatMap(hasRequired => {

          if (hasRequired) {
            val symbol = if (url.contains("?")) "&" else "?"

            val ticket = "ST-" + RandomUtils.randomString(64)

            val redirectUrl = tickets
              .insertCasTicket(ticket, userId, serviceId)
              .map(_ => url + symbol + "ticket=" + ticket)

            if (!url.startsWith("https://")) {
              // Security for weird redirects, specially for android apps
              redirectUrl.map(url => Ok(views.html.redirectConfirm(url)))
            } else {
              redirectUrl.map(url => Redirect(url))
            }
          } else {
            Forbidden(views.html.errorPage("Permissions manquantes", Html("<p>L'accès à cette application nécessite d'être membre de certains groupes.</p>")))
          }
        })
      case TokensInstance(redirectUrl, safe) =>
        groups.getGroupsByMember(userId)
          .flatMap(groups => sessions.createSession(userId).map(sessionId => (sessionId, groups)))
          .map {
            case (sid, groups) =>
              val token = jwt.issueToken(userId, groups.map(_.name).toSet)
              val refresh = sid.toString

              redirectUrl + "?accessToken=" + token + "&refreshToken=" + refresh + "&duration=" + (jwt.ExpirationTimeMinutes * 60)
          }
          .map(url => {
            if (!url.startsWith("https://") || !safe) {
              Ok(views.html.redirectConfirm(url))
            } else {
              Redirect(url)
            }
          })

    }).map(result => result.removingFromSession(AuthenticationInstance.SessionKey))
  }

  def redirectGet: Action[AnyContent] = Action.async { implicit rq =>
    if (rq.hasUserSession) {
      // Check: are required informations provided?

      users.getUserProfile(rq.userSession.id).flatMap {
        case Some((user, Some(address))) =>
          // has address
          val authInstance = rq.session.get(AuthenticationInstance.SessionKey)
            .map(Json.parse)
            .flatMap(js => Json.fromJson[AuthenticationInstance](js).asOpt)

          println("Redirect: user " + user.email + " with authInstance " + authInstance)

          this.produceRedirection(user, address, authInstance)
        case _ =>
          Redirect(controllers.forms.routes.UpdateInfoController.updateGet())
      }
    } else {
      Redirect(controllers.forms.routes.LoginController.loginGet())
    }
  }


}
