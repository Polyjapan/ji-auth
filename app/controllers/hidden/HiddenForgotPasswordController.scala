package controllers.hidden

import java.net.URLEncoder
import java.sql.Timestamp

import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import javax.inject.Inject
import models.{AppsModel, ReCaptchaModel, UsersModel}
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._
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
                                                captcha: ReCaptchaModel)(implicit ec: ExecutionContext, mailer: MailerClient) extends MessagesAbstractController(cc) with I18nSupport {


  /**
    * The format of the request sent by the client
    *
    * @param email    the email of the user
    * @param clientId the clientId of the app
    * @param captcha  the value returned by recaptcha
    */
  case class ForgotPasswordRequest(email: String, clientId: String, captcha: String)

  implicit val format: Reads[ForgotPasswordRequest] = Json.reads[ForgotPasswordRequest]

  def postForgotPassword: Action[ForgotPasswordRequest] = Action.async(parse.json[ForgotPasswordRequest]) { implicit rq =>
    if (rq.hasBody) {
      val body = rq.body

      // Get client first
      apps getApp body.clientId flatMap {
        case Some(app) =>

          // Check that the captcha is correct
          captcha.checkCaptchaWithExpiration(app, body.captcha).flatMap(captchaResponse => {
            if (!captchaResponse.success) !InvalidCaptcha
            else users.getUser(body.email).map {

              // Get the client
              case Some(user) =>

                // We have a client, create a code and a reset URL
                val resetCode = RandomUtils.randomString(32)
                val resetCodeEncoded = URLEncoder.encode(resetCode, "UTF-8")
                val emailEncoded = URLEncoder.encode(user.email, "UTF-8")

                val url = app.emailRedirectUrl + "?email=" + emailEncoded + "&action=passwordReset&resetCode=" + resetCodeEncoded

                // Update the user with the code, and a validity expiration of 24hrs
                users.updateUser(user.copy(
                  passwordReset = Some(resetCode),
                  passwordResetEnd = Some(new Timestamp(System.currentTimeMillis + (24 * 3600 * 1000)))))
                  .onComplete(_ => {
                    mailer.send(Email(
                      rq.messages("users.recover.email_title"),
                      rq.messages("users.recover.email_from") + " <noreply@japan-impact.ch>",
                      Seq(user.email),
                      bodyText = Some(rq.messages("users.recover.email_text", url))
                    ))
                  })

                Ok
              case None =>
                // We have no user with that email, send an email to the user to warn him
                mailer.send(Email(
                  rq.messages("users.recover.email_title"),
                  rq.messages("users.recover.email_from") + " <noreply@japan-impact.ch>",
                  Seq(body.email),
                  bodyText = Some(rq.messages("users.recover.no_user_email_text"))
                ))

                Ok
            }
          })


        case None => !UnknownApp // No body or body parse fail ==> invalid input
      }
    }

    else !MissingData // No body or body parse fail ==> invalid input
  }


}
