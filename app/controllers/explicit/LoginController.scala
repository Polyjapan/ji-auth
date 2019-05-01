package controllers.explicit

import data.UserSession
import javax.inject.Inject
import models.{AppsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import play.twirl.api.HtmlFormat

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class LoginController @Inject()(cc: MessagesControllerComponents)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration,
                                                                  tickets: TicketsModel,
                                                                  users: UsersModel,
                                                                  apps: AppsModel) extends MessagesAbstractController(cc) with I18nSupport {


  private val loginForm = Form(mapping("email" -> email, "password" -> nonEmptyText(8))(Tuple2.apply)(Tuple2.unapply))

  private def displayForm(form: Form[(String, String)], app: Option[String])(implicit rq: RequestHeader): Future[HtmlFormat.Appendable] =
    apps.getAppName(app).map(name =>
      views.html.login.login(form,
        if (name.isDefined) app else None, // Don't re-use an invalid clientId :)
        name)
    )


  def loginGet(app: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    ExplicitTools.ifLoggedOut(app) {
      displayForm(loginForm, app).map(f => Ok(f))
    }
  }

  def loginPost(app: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    ExplicitTools.ifLoggedOut(app) {
      loginForm.bindFromRequest().fold(withErrors => {
        displayForm(withErrors, app).map(f => BadRequest(f))
      }, data => {
        val (email, password) = data

        users.login(email, password).flatMap({

          case users.BadLogin =>
            displayForm(loginForm.withGlobalError("Email ou mot de passe incorrect"), app).map(f => BadRequest(f))
          case users.EmailNotConfirmed =>
            displayForm(loginForm.withGlobalError("Vous devez confirmer votre adresse email pour pouvoir vous connecter"), app).map(f => BadRequest(f))
          case users.LoginSuccess(user) =>
            ExplicitTools.produceRedirectUrl(app, user.id.get)
              .map(url => Redirect(url).withSession(UserSession(user): _*))
        })
      })
    }
  }


}
