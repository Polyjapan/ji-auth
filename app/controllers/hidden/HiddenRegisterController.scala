package controllers.hidden

import java.net.URLEncoder

import ch.japanimpact.auth.api.TicketType
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import data.LoginSuccess
import javax.inject.Inject
import models.{AppsModel, HashModel, ReCaptchaModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.data.validation._
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._
import utils.Implicits._
import utils.{RandomUtils, ValidationUtils}

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

  val InvalidEmail = 201
  val PasswordTooShort = 202


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

      if (!ValidationUtils.isValidEmail(body.email)) {
        !InvalidEmail
      } else if (!ValidationUtils.isValidPassword(body.password)) {
        !PasswordTooShort
      } else apps getApp body.clientId flatMap {
        case Some(app) =>
          users.register(
            body.captcha, app.recaptchaPrivate,
            body.email, body.password,
            (email, code) => app.emailRedirectUrl + "?email=" + email + "&action=confirmEmail&confirmCode=" + code
          ).map {
            case users.BadCaptcha => !InvalidCaptcha
            case users.AlreadyRegistered(id) => (id, TicketType.DoubleRegisterTicket)
            case users.AccountCreated(id) => (id, TicketType.RegisterTicket)
          }.flatMap {
            case (id: Int, tt: TicketType) =>
              tickets.createTicketForUser(id, app.id.get, tt) map LoginSuccess.apply map toOkResult[LoginSuccess]
            case f: Future[Result] => f
            case f: Result => Future(f)
          }
        case None => !UnknownApp // No body or body parse fail ==> invalid input
      }
    } else !MissingData // No body or body parse fail ==> invalid input
  }


}
