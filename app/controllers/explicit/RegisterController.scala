package controllers.explicit

import javax.inject.Inject
import models.{AppsModel, HashModel, ReCaptchaModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import play.twirl.api.HtmlFormat
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class RegisterController @Inject()(cc: MessagesControllerComponents,
                                   users: UsersModel,
                                   hashes: HashModel,
                                   captcha: ReCaptchaModel)(implicit ec: ExecutionContext, apps: AppsModel, tickets: TicketsModel, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) with I18nSupport {

  private val registerForm = Form(mapping("email" -> email, "password" -> nonEmptyText(8), "g-recaptcha-response" -> text)(Tuple3.apply)(Tuple3.unapply))

  private def displayForm(form: Form[(String, String, String)], app: Option[String])(implicit rq: RequestHeader): Future[HtmlFormat.Appendable] =
    apps.getAppName(app).map(name =>
      views.html.register.register(form, config.get[String]("recaptcha.siteKey"),
        if (name.isDefined) app else None, // Don't re-use an invalid clientId :)
        name)
    )

  def registerGet(app: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    ExplicitTools.ifLoggedOut(app) {
      displayForm(registerForm, app).map(f => Ok(f))
    }
  }

  def registerPost(app: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    ExplicitTools.ifLoggedOut(app) {
      registerForm.bindFromRequest().fold(withErrors => {
        displayForm(withErrors, app).map(f => BadRequest(f))
      }, data => {
        val (email, password, captchaResponse) = data

        users.register(
          captchaResponse, Some(captcha.AuthSecretKey),
          email, password, (email, code) => controllers.explicit.routes.EmailConfirmController.emailConfirmGet(email, code, app).absoluteURL(true)
        ).flatMap({
          case users.BadCaptcha =>
            displayForm(registerForm.withGlobalError("Captcha incorrect"), app).map(f => BadRequest(f))
          case _ =>
            Ok(views.html.register.registerok(email))
        })
      })
    }
  }


}
