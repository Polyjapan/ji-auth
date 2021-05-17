package controllers.forms

import data.UserSession
import models.tfa.TFAModel

import javax.inject.Inject
import models.{ServicesModel, SessionsModel, UsersModel}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import play.twirl.api.HtmlFormat

import scala.concurrent.{ExecutionContext, Future}

object LoginController {
  val TemporarySessionKey = "temporarySession"
}
/**
 * @author Louis Vialar
 */
class LoginController @Inject()(cc: MessagesControllerComponents)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration,
                                                                  users: UsersModel, sessions: SessionsModel, tfa: TFAModel,
                                                                  apps: ServicesModel) extends MessagesAbstractController(cc) with I18nSupport {


  private val loginForm = Form(mapping("email" -> email, "password" -> nonEmptyText(8))(Tuple2.apply)(Tuple2.unapply))

  def loginGet(app: Option[String], tokenType: Option[String], service: Option[String] = None): Action[AnyContent] = Action.async { implicit rq: Request[AnyContent] =>
    val flashErrors = rq.flash.get("errors")
    val flashSuccess = rq.flash.get("resend-success").contains("true")

    AuthTools.ifLoggedOut {
      Future.successful(Ok(views.html.login.login(
        flashErrors match {
          case Some(err) => loginForm.withGlobalError(err)
          case None => loginForm
        }, resendSuccess = flashSuccess
      )))
    }
  }

  def emailReconfirm(email: String): Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedOut {
      users.resendConfirmEmail(email,
        (email, code) => controllers.forms.routes.EmailConfirmController.emailConfirmGet(email, code).absoluteURL(true)) map {
        case users.Success =>
          Redirect(controllers.forms.routes.LoginController.loginGet(None, None))
            .flashing("resend-success" -> "true")
        case users.NoAccountOrAlreadyConfirmed =>
          Redirect(controllers.forms.routes.LoginController.loginGet(None, None))
          .flashing("errors" -> "Ce compte n'existe pas ou l'email est déjà confirmé")
        case users.RetryLater =>
          Redirect(controllers.forms.routes.LoginController.loginGet(None, None))
          .flashing("errors" -> "Merci de patienter environ 5 minutes avant de demander un nouvel email.")
      }
    }
  }

  def loginPost: Action[AnyContent] = Action.async { implicit rq: Request[AnyContent] =>
    AuthTools.ifLoggedOut {
      loginForm.bindFromRequest().fold(withErrors => {
        Future.successful(BadRequest(views.html.login.login(withErrors)))
      }, data => {
        val (email, password) = data

        users.login(email, password).map {
          case users.BadLogin =>
            BadRequest(views.html.login.login(loginForm.withGlobalError("Email ou mot de passe incorrect")))
          case users.EmailNotConfirmed(canRetry: Boolean) =>
            BadRequest(
              views.html.login.login(loginForm.withGlobalError("Vous devez confirmer votre adresse email pour pouvoir vous connecter"),
                resendConfirmEmail = if (canRetry) Some(email) else None
            ))
          case users.LoginSuccess(user) =>
            TFAValidationController.writeTemporarySession(user.id.get)(Redirect(controllers.forms.routes.TFAValidationController.tfaCheckGet()))
            /*sessions.createSession(user.id.get, rq.remoteAddress, rq.headers.get("User-Agent").getOrElse("unknown"))
              .map(sid => Redirect(controllers.routes.RedirectController.redirectGet()).addingToSession(UserSession(user, sid): _*))*/
        }
      })
    }
  }


}
