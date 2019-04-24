package controllers.hidden

import java.net.URLEncoder

import ch.japanimpact.auth.api.TicketType
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import data.LoginSuccess
import javax.inject.Inject
import models.{AppsModel, HashModel, ReCaptchaModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._
import utils.Implicits._
import utils.RandomUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class HiddenRegisterController @Inject()(
                                          cc: MessagesControllerComponents,
                                          users: UsersModel,
                                          tickets: TicketsModel,
                                          apps: AppsModel,
                                          captcha: ReCaptchaModel,
                                          hashes: HashModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) with I18nSupport {

  /**
    * The format of the request sent by the client
    *
    * @param email    the email of the user
    * @param password the password of the user
    * @param captcha  the value of the captcha
    * @param clientId the clientId of the app
    */
  case class RegisterRequest(email: String, captcha: String, password: String, clientId: String)

  implicit val format: Reads[RegisterRequest] = Json.reads[RegisterRequest]

  def postRegister: Action[RegisterRequest] = Action.async(parse.json[RegisterRequest]) { implicit rq =>
    if (rq.hasBody) {
      val body = rq.body

      // Get client first
      apps getApp body.clientId flatMap {
        case Some(app) =>

          captcha.checkCaptchaWithExpiration(app, body.captcha).flatMap(result =>
            if (!result.success) !InvalidCaptcha

            // If we have no error in the form itself we try to find the user data
            else users.getUser(body.email).flatMap {
              case Some(user) =>
                mailer.send(Email(
                  rq.messages("users.register.exists.email_title"),
                  rq.messages("users.register.exists.email_from") + " <noreply@japan-impact.ch>",
                  Seq(body.email),
                  bodyText = Some(rq.messages("users.register.exists.email_text"))
                ))

                Future((user.id.get, TicketType.DoubleRegisterTicket))
              case None =>

                val confirmCode = RandomUtils.randomString(32)
                val emailEncoded = URLEncoder.encode(body.email, "UTF-8")

                val url = app.emailRedirectUrl + "?email=" + emailEncoded + "&action=confirmEmail&confirmCode=" + URLEncoder.encode(confirmCode, "UTF-8")
                val (algo, hash) = hashes.hash(body.password)

                mailer.send(Email(
                  rq.messages("users.register.email_title"),
                  rq.messages("users.register.email_from") + " <noreply@japan-impact.ch>",
                  Seq(body.email),
                  bodyText = Some(rq.messages("users.register.email_text", url))
                ))

                users
                  .createUser(data.RegisteredUser(Option.empty, body.email, Some(confirmCode), hash, algo))
                  .map(id => (id, TicketType.RegisterTicket))
            }.flatMap {
              case (userId, tt) =>
                tickets.createTicketForUser(userId, app.id.get, tt) map LoginSuccess.apply map toOkResult[LoginSuccess]
            }
          )

        case None => !UnknownApp // No body or body parse fail ==> invalid input
      }
    } else !MissingData // No body or body parse fail ==> invalid input
  }


}
