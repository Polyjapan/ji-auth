package controllers.hidden

import java.net.URLEncoder
import java.sql.Timestamp

import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import javax.inject.Inject
import models.{AppsModel, UsersModel}
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._
import services.ReCaptchaService
import utils.Implicits._
import utils.RandomUtils

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class HiddenForgotPasswordController @Inject()(
                                                cc: MessagesControllerComponents,
                                                users: UsersModel,
                                                apps: AppsModel,
                                                captcha: ReCaptchaService)(implicit ec: ExecutionContext, mailer: MailerClient) extends MessagesAbstractController(cc) with I18nSupport {


  /**
    * The format of the request sent by the client
    *
    * @param email    the email of the user
    * @param clientId the clientId of the app
    * @param captcha  the value returned by recaptcha
    */
  case class ForgotPasswordRequest(email: String, clientId: String, captcha: String)

  implicit val format: Reads[ForgotPasswordRequest] = Json.reads[ForgotPasswordRequest]

  def postForgotPassword: Action[ForgotPasswordRequest] = Action.async(parse.json[ForgotPasswordRequest]) { implicit rq: Request[ForgotPasswordRequest] =>
    if (rq.hasBody) {
      val body = rq.body

      // Get client first
      apps getApp body.clientId flatMap {
        case Some(app) =>

          // Check that the captcha is correct
          captcha.checkCaptchaWithExpiration(app, body.captcha).flatMap(captchaResponse => {
            if (!captchaResponse.success) !InvalidCaptcha
            else {
              users
                .resetPassword(body.email, (email, code) =>
                  app.emailRedirectUrl + "?email=" + email + "&action=passwordReset&resetCode=" + code)
                .map(_ => Ok)
            }
          })
        case None => !UnknownApp // No body or body parse fail ==> invalid input
      }
    }

    else !MissingData // No body or body parse fail ==> invalid input
  }


}
