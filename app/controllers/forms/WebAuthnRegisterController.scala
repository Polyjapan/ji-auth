package controllers.forms

import data.UserSession
import models.{ServicesModel, SessionsModel, UsersModel, WebAuthnModel}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import play.twirl.api.HtmlFormat

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class WebAuthnRegisterController @Inject()(cc: MessagesControllerComponents)(implicit ec: ExecutionContext,
                                                                             users: UsersModel,
                                                                             webAuthn: WebAuthnModel) extends MessagesAbstractController(cc) with I18nSupport {


  private val loginForm = Form(mapping("email" -> email, "password" -> nonEmptyText(8))(Tuple2.apply)(Tuple2.unapply))

  private def displayForm(form: Form[(String, String)])(implicit rq: RequestHeader): HtmlFormat.Appendable = views.html.login.login(form)

  def registerGet: Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedIn { user: UserSession =>
      users.getUserById(user.id).flatMap {
        case Some(user) => webAuthn.startRegistration(user)
        case None => Future.failed(new NullPointerException)
      }.map {
        case (json, uid) => views.html.webauthn.register(uid.toString, json)
      }
    }
  }

  def loginPost: Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedOut {
      loginForm.bindFromRequest().fold(withErrors => {
        Future.successful(BadRequest(displayForm(withErrors)))
      }, data => {
        val (email, password) = data

        users.login(email, password).flatMap {
          case users.BadLogin =>
            Future.successful(BadRequest(displayForm(loginForm.withGlobalError("Email ou mot de passe incorrect"))))
          case users.EmailNotConfirmed =>
            Future.successful(BadRequest(displayForm(loginForm.withGlobalError("Vous devez confirmer votre adresse email pour pouvoir vous connecter"))))
          case users.LoginSuccess(user) =>
            sessions.createSession(user.id.get, rq.remoteAddress, rq.headers.get("User-Agent").getOrElse("unknown"))
              .map(sid => Redirect(controllers.routes.RedirectController.redirectGet()).addingToSession(UserSession(user, sid): _*))
        }
      })
    }
  }


}
