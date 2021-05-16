package controllers

import ch.japanimpact.auth.api.UserData
import controllers.saml2._
import data.UserSession._
import data.{AuthenticationInstance, CASInstance, SAMLv2Instance, SessionID}
import models._
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import services.JWTService
import utils.Implicits._
import utils.RandomUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class RedirectController @Inject()(cc: MessagesControllerComponents)
                                  (implicit ec: ExecutionContext, config: Configuration,
                                   users: UsersModel, tickets: TicketsModel, groups: GroupsModel,
                                   sessions: SessionsModel, jwt: JWTService,
                                   apps: ServicesModel, saml: SAMLResponseBuilder) extends MessagesAbstractController(cc) with I18nSupport {


  def redirectGet: Action[AnyContent] = Action.async { implicit rq =>
    if (rq.hasUserSession) {
      sessions.getSession(rq.userSession.sessionKey).flatMap {
        case Some(user) =>

          val authInstance = rq.session.get(AuthenticationInstance.SessionKey)
            .map(Json.parse)
            .flatMap(js => Json.fromJson[AuthenticationInstance](js).asOpt)

          println("Try to redirect: user " + user.email + " with authInstance " + authInstance)

          this.produceRedirection(user, rq.userSession.sessionKey, authInstance)
        case _ =>
          Redirect(controllers.forms.routes.LoginController.loginGet()).removingFromSession("id", "email", "sessionKey")
      }
    } else {
      Redirect(controllers.forms.routes.LoginController.loginGet())
    }
  }

  private def produceRedirection(user: UserData,
                                 sessionId: SessionID,
                                 authInstance: Option[AuthenticationInstance])(implicit rq: RequestHeader): Future[Result] = {
    val userId = user.id.get

    (if (authInstance.isEmpty) Redirect("/").asFuture
    else authInstance.get match {
      case CASInstance(url, serviceId, requireInfo) =>
        if (requireInfo && (user.details.phoneNumber.isEmpty || user.address.isEmpty)) {
          // The service requires more data but the user didn't provide it yet: we must request it.
          Future.successful(Redirect(controllers.forms.routes.UpdateInfoController.updateGet(Some(true))))
        } else {
          // check that the user has all required groups to access the app
          apps.hasRequiredGroups(serviceId, userId).flatMap(hasRequired => {
            if (hasRequired) {
              val symbol = if (url.contains("?")) "&" else "?"

              val ticket = "ST-" + RandomUtils.randomString(64)

              val redirectUrl = tickets
                .insertCasTicket(ticket, userId, serviceId, sessionId)
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
          }) // in any case, the redirection is done now, so we invalidate it
            .map(result => result.removingFromSession(AuthenticationInstance.SessionKey))
        }
      case samlInstance: SAMLv2Instance =>

        if (samlInstance.requireFullInfo && (user.details.phoneNumber.isEmpty || user.address.isEmpty)) {
          // The service requires more data but the user didn't provide it yet: we must request it.
          Future.successful(Redirect(controllers.forms.routes.UpdateInfoController.updateGet(Some(true))))
        } else {
          val url = samlInstance.url

          val samlResponse = saml.success(samlInstance, user)

          if (samlInstance.binding == SAMLBindings.HTTPRedirect) {
            val params = Map("SAMLResponse" -> Seq(samlResponse)) ++ (samlInstance.relay.map(r => ("RelayState", Seq(r))))

            if (url.startsWith("https://")) {
              Future successful Redirect(url, params)
            } else {
              val qStr = params.map(pair => pair._1 + "=" + pair._2.head).mkString("&")
              Ok(views.html.redirectConfirm(url + "?" + qStr))
            }
          } else if (samlInstance.binding == SAMLBindings.HTTPPost) {
            val safeUrl = url.startsWith("https://")

            Ok(views.html.redirectSaml(url, samlResponse, samlInstance.relay, safeUrl))
          } else {
            Future successful BadRequest(views.html.errorPage("Binding non supporté", Html("<p>Ce binding SAML n'est malheureusement pas supporté...</p>")))
          }
        }
    })
  }


}
