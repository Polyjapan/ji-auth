package controllers.explicit

import javax.inject.Inject
import models.{AppsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import play.twirl.api.HtmlFormat
import services.{HashService, ReCaptchaService}
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class RegisterController @Inject()(cc: MessagesControllerComponents,
                                   users: UsersModel,
                                   hashes: HashService,
                                   captcha: ReCaptchaService)(implicit ec: ExecutionContext, apps: AppsModel, tickets: TicketsModel, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) with I18nSupport {

  private val registerForm = Form(
    mapping(
      "email" -> email, "password" -> nonEmptyText(8),
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "phone" -> optional(nonEmptyText),

      "address" -> nonEmptyText,
      "addressComplement" -> optional(nonEmptyText),
      "postCode" -> nonEmptyText,
      "region" -> nonEmptyText,
      "country" -> nonEmptyText,

      "g-recaptcha-response" -> text)(Tuple11.apply)(Tuple11.unapply))

  private def displayForm(form: Form[_], app: Option[String])(implicit rq: RequestHeader): Future[HtmlFormat.Appendable] =
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
        val (email, password, firstName, lastName, phone, address, addressComplement, postCode, region, country, captchaResponse) = data

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
