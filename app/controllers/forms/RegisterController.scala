package controllers.forms

import data._
import javax.inject.Inject
import models.{ServicesModel, UsersModel}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import play.twirl.api.HtmlFormat
import services.{HashService, ReCaptchaService}
import utils.ValidationUtils

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class RegisterController @Inject()(cc: MessagesControllerComponents, hashes: HashService, captcha: ReCaptchaService)
                                  (implicit ec: ExecutionContext, apps: ServicesModel, users: UsersModel, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) with I18nSupport {

  private val registerForm = Form(
    mapping(
      "email" -> email, "password" -> nonEmptyText(8),
      "firstName" -> nonEmptyText(1, 50),
      "lastName" -> nonEmptyText(1, 50),
      "newsletter" -> boolean,
      "g-recaptcha-response" -> text)(Tuple6.apply)(Tuple6.unapply))

  private def displayForm(form: Form[_])(implicit rq: RequestHeader): HtmlFormat.Appendable =
    views.html.register.register(form, config.get[String]("recaptcha.siteKey"))


  def registerGet: Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedOut {
      Future.successful(Ok(displayForm(registerForm)))
    }
  }

  def registerPost: Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedOut {
      registerForm.bindFromRequest().fold(withErrors => {
        println(withErrors.errors)
        Future.successful(BadRequest(displayForm(withErrors)))
      }, data => {
        val (email, password, firstName, lastName, newsletter, captchaResponse) = data

        // Password is hashed by register method, don't worry
        val profile = RegisteredUser(None, email, None, password, null, firstName = firstName, lastName = lastName, phoneNumber = None, newsletter = newsletter)

        users.register(
          captchaResponse, Some(captcha.AuthSecretKey),
          profile, None, (email, code) => controllers.forms.routes.EmailConfirmController.emailConfirmGet(email, code).absoluteURL(true)
        ).map {
          case users.BadCaptcha =>
            BadRequest(displayForm(registerForm.withGlobalError("Captcha incorrect")))
          case _ =>
            Ok(views.html.register.registerok(email))
        }
      })
    }
  }


}
