package controllers.explicit

import data.{AuthenticationInstance, TicketsInstance, TokensInstance, UserSession}
import javax.inject.Inject
import models.{AppsModel, UsersModel}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import play.twirl.api.{Html, HtmlFormat}

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class LoginController @Inject()(cc: MessagesControllerComponents)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration,
                                                                  users: UsersModel,
                                                                  apps: AppsModel) extends MessagesAbstractController(cc) with I18nSupport {


  private val loginForm = Form(mapping("email" -> email, "password" -> nonEmptyText(8))(Tuple2.apply)(Tuple2.unapply))

  private def displayForm(form: Form[(String, String)])(implicit rq: RequestHeader): HtmlFormat.Appendable = views.html.login.login(form)

  def loginGet(app: Option[String], tokenType: Option[String], service: Option[String] = None): Action[AnyContent] = Action.async { implicit rq =>
    // START adapter for old services
    if (service.nonEmpty) {
      Future.successful(Redirect(controllers.cas.routes.CASLoginController.loginGet(service.get)))
    } else {
      if (app.nonEmpty) {
        apps.getApp(app.get).flatMap {
          case Some(app) =>
            val sess: AuthenticationInstance = tokenType match {
              case Some("token") => TokensInstance(app.redirectUrl)
              case _ => TicketsInstance(app.appId.get, app.redirectUrl)
            }


            // TODO: Treat tickets service as a CAS

            // TODO: move in own place

            ExplicitTools.ifLoggedOut {
              Future.successful(Ok(displayForm(loginForm)))
            }.map(r => r.addingToSession(sess.pair))
          case None =>
            Future.successful(NotFound(views.html.errorPage("Application introuvable", Html("<p>L'application spécifiée est introuvable. Merci de signaler cette erreur au créateur du site dont vous provenez."))))
        }
      } else {

        // END Adapter for old services
        ExplicitTools.ifLoggedOut {
          Future.successful(Ok(displayForm(loginForm)))
        }
      }
    }
  }

  def loginPost: Action[AnyContent] = Action.async { implicit rq =>
    ExplicitTools.ifLoggedOut {
      loginForm.bindFromRequest().fold(withErrors => {
        Future.successful(BadRequest(displayForm(withErrors)))
      }, data => {
        val (email, password) = data

        users.login(email, password).map{

          case users.BadLogin =>
            BadRequest(displayForm(loginForm.withGlobalError("Email ou mot de passe incorrect")))
          case users.EmailNotConfirmed =>
            BadRequest(displayForm(loginForm.withGlobalError("Vous devez confirmer votre adresse email pour pouvoir vous connecter")))
          case users.LoginSuccess(user) =>
            Redirect(controllers.forms.routes.RedirectController.redirectGet()).addingToSession(UserSession(user): _*)
        }
      })
    }
  }


}
