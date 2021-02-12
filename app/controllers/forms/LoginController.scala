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

  private def displayForm(form: Form[(String, String)])(implicit rq: RequestHeader): HtmlFormat.Appendable = views.html.login.login(form)

  def loginGet(app: Option[String], tokenType: Option[String], service: Option[String] = None): Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedOut {
      Future.successful(Ok(displayForm(loginForm)))
    }
  }

  def loginPost: Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedOut {
      loginForm.bindFromRequest().fold(withErrors => {
        Future.successful(BadRequest(displayForm(withErrors)))
      }, data => {
        val (email, password) = data

        users.login(email, password).map {
          case users.BadLogin =>
            BadRequest(displayForm(loginForm.withGlobalError("Email ou mot de passe incorrect")))
          case users.EmailNotConfirmed =>
            BadRequest(displayForm(loginForm.withGlobalError("Vous devez confirmer votre adresse email pour pouvoir vous connecter")))
          case users.LoginSuccess(user) =>
            TFAValidationController.writeTemporarySession(user.id.get)(Redirect(controllers.forms.routes.TFAValidationController.tfaCheckGet()))
            /*sessions.createSession(user.id.get, rq.remoteAddress, rq.headers.get("User-Agent").getOrElse("unknown"))
              .map(sid => Redirect(controllers.routes.RedirectController.redirectGet()).addingToSession(UserSession(user, sid): _*))*/
        }
      })
    }
  }


}
