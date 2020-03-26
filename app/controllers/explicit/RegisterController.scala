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
import data._
import utils.ValidationUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class RegisterController @Inject()(cc: MessagesControllerComponents,
                                   hashes: HashService, ExplicitTools: ExplicitTools,
                                   captcha: ReCaptchaService)(implicit ec: ExecutionContext, apps: AppsModel, users: UsersModel, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) with I18nSupport {

  private val registerForm = Form(
    mapping(
      "email" -> email, "password" -> nonEmptyText(8),
      "firstName" -> nonEmptyText(1, 50),
      "lastName" -> nonEmptyText(1, 50),
      "phone" -> optional(ValidationUtils.validPhoneVerifier(nonEmptyText(8, 20))),

      "address" -> nonEmptyText(2, 200),
      "addressComplement" -> optional(nonEmptyText(2, 200)),
      "postCode" -> nonEmptyText(3, 10),
      "city" -> nonEmptyText(3, 100),
      "country" -> nonEmptyText(2, 100),

      "g-recaptcha-response" -> text)(Tuple11.apply)(Tuple11.unapply))

  private def displayForm(form: Form[_], app: Option[String], tokenType: Option[String])(implicit rq: RequestHeader): Future[HtmlFormat.Appendable] =
    apps.getAppName(app).map(name =>
      views.html.register.register(form, config.get[String]("recaptcha.siteKey"),
        if (name.isDefined) app else None, // Don't re-use an invalid clientId :)
        name, tokenType)
    )

  def registerGet(app: Option[String], tokenType: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    ExplicitTools.ifLoggedOut(app, tokenType) {
      displayForm(registerForm, app, tokenType).map(f => Ok(f))
    }
  }

  def registerPost(app: Option[String], tokenType: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    ExplicitTools.ifLoggedOut(app, tokenType) {
      registerForm.bindFromRequest().fold(withErrors => {
        println(withErrors.errors)
        displayForm(withErrors, app, tokenType).map(f => BadRequest(f))
      }, data => {
        val (email, password, firstName, lastName, phone, address, addressComplement, postCode, city, country, captchaResponse) = data

        val addr = Address(-1, address, addressComplement, postCode, city, country)
        // Password is hashed by register method, don't worry
        val profile = RegisteredUser(None, email, None, password, null, firstName = firstName, lastName = lastName, phoneNumber = phone)

        users.register(
          captchaResponse, Some(captcha.AuthSecretKey),
          profile, Some(addr), (email, code) => controllers.explicit.routes.EmailConfirmController.emailConfirmGet(email, code, app, tokenType).absoluteURL(true)
        ).flatMap({
          case users.BadCaptcha =>
            displayForm(registerForm.withGlobalError("Captcha incorrect"), app, tokenType).map(f => BadRequest(f))
          case _ =>
            Ok(views.html.register.registerok(email))
        })
      })
    }
  }


}
