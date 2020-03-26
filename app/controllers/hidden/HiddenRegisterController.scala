package controllers.hidden

import ch.japanimpact.auth.api.{LoginSuccess, TicketType}
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import data.{Address, RegisteredUser}
import javax.inject.Inject
import models.{AppsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import services.{HashService, ReCaptchaService}
import utils.Implicits._
import utils.ValidationUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class HiddenRegisterController @Inject()(
                                          cc: MessagesControllerComponents,
                                          users: UsersModel,
                                          tickets: TicketsModel,
                                          apps: AppsModel,
                                          captcha: ReCaptchaService,
                                          hashes: HashService)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) with I18nSupport {

  val InvalidEmail = 201
  val PasswordTooShort = 202
  val PhoneInvalid = 203

  val AllowPartialRegisters = config.getOptional[Boolean]("api.allowPartialRegisters").getOrElse(true)

  /**
    * The format of the request sent by the client
    *
    * @param email    the email of the user
    * @param password the password of the user
    * @param captcha  the value of the captcha
    * @param clientId the clientId of the app
    */
  case class RegisterRequest(email: String, captcha: String, password: String, clientId: String,
                             firstName: String,
                             lastName: String,
                             phone: Option[String],
                             address: Option[RegisterAddress]
                            ) {

    def toUser = RegisteredUser(None, email, None, password, null, firstName = firstName, lastName = lastName, phoneNumber = phone)

    def isLegal = if (AllowPartialRegisters) true else address.nonEmpty && firstName.nonEmpty && lastName.nonEmpty
  }

  case class RegisterAddress(address: String, complement: Option[String], postCode: String, city: String, country: String) {
    def toAddress = Address(-1, address, complement, postCode, city, country)
  }

  implicit val formatRegisterAddress: Reads[RegisterAddress] = Json.reads[RegisterAddress]
  implicit val format: Reads[RegisterRequest] = Json.reads[RegisterRequest]

  def postRegister: Action[RegisterRequest] = Action.async(parse.json[RegisterRequest]) { implicit rq =>
    if (rq.hasBody && rq.body.isLegal) {
      val body = rq.body

      if (!ValidationUtils.isValidEmail(body.email)) {
        !InvalidEmail
      } else if (!ValidationUtils.isValidPassword(body.password)) {
        !PasswordTooShort
      } else if (body.phone.isDefined && !ValidationUtils.isValidPhone(body.phone.get)) {
        !PhoneInvalid
      } else apps getApp body.clientId flatMap {
        case Some(app) =>
          users.register(
            body.captcha, app.recaptchaPrivateKey, body.toUser, body.address.map(_.toAddress),
            (email, code) => app.emailCallbackUrl + "?email=" + email + "&action=confirmEmail&confirmCode=" + code
          ).map {
            case users.BadCaptcha => !InvalidCaptcha
            case users.AlreadyRegistered(id) => (id, TicketType.DoubleRegisterTicket)
            case users.AccountCreated(id) => (id, TicketType.RegisterTicket)
          }.flatMap {
            case (id: Int, tt: TicketType) =>
              tickets.createTicketForUser(id, app.appId.get, tt) map LoginSuccess.apply map toOkResult[LoginSuccess]
            case f: Future[Result] => f
            case f: Result => Future(f)
          }
        case None => !UnknownApp // No body or body parse fail ==> invalid input
      }
    } else !MissingData // No body or body parse fail ==> invalid input
  }


}
