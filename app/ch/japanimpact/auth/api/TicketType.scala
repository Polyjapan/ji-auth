package ch.japanimpact.auth.api

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

/**
 * @author Louis Vialar
 */
@deprecated
trait TicketType {
  /**
   * A boolean indicating whether this ticket represents a successful login and can be used to authorize an user
   */
  val isValidLogin: Boolean = false
}

@deprecated
object TicketType {
  def apply(s: String): TicketType = s match {
    case "T_LOGIN" => LoginTicket
    case "T_REGISTER" => RegisterTicket
    case "T_DOUBLE_REGISTER" => DoubleRegisterTicket
    case "T_EMAIL_CONFIRM" => EmailConfirmTicket
    case "T_PASSWORD_RESET" => PasswordResetTicket
    case "T_EXPLICIT_GRANT" => ExplicitGrantTicket
    case "T_APP" => AppTicket
  }

  def unapply(ticketType: TicketType): String = ticketType match {
    case LoginTicket => "T_LOGIN"
    case RegisterTicket => "T_REGISTER"
    case DoubleRegisterTicket => "T_DOUBLE_REGISTER"
    case EmailConfirmTicket => "T_EMAIL_CONFIRM"
    case PasswordResetTicket => "T_PASSWORD_RESET"
    case ExplicitGrantTicket => "T_EXPLICIT_GRANT"
    case AppTicket => "T_APP"
  }

  /**
   * A ticket emitted when the user successfully logged in using its email and password
   */
  case object LoginTicket extends TicketType {
    override val isValidLogin: Boolean = true
  }

  /**
   * A ticket emitted when the user successfully registered against the system but did not confirm its email yet
   */
  case object RegisterTicket extends TicketType

  /**
   * A ticket emitted when the user tried to register using an email that already exists in the system
   */
  case object DoubleRegisterTicket extends TicketType

  /**
   * A ticket emitted when the user just confirmed his email address
   */
  case object EmailConfirmTicket extends TicketType {
    override val isValidLogin: Boolean = true
  }

  /**
   * A ticket emitted when the user just reset his password using a previously sent link
   */
  case object PasswordResetTicket extends TicketType {
    override val isValidLogin: Boolean = true
  }

  /**
   * A ticket emitted when the user is sent to an app using the explicit mode
   */
  case object ExplicitGrantTicket extends TicketType {
    override val isValidLogin: Boolean = true
  }

  /**
   * A ticket emitted when an app requests a ticket for itself. It means the caller is not an
   * user but an app.
   */
  case object AppTicket extends TicketType {
    override val isValidLogin: Boolean = true
  }

  implicit val sourceFormat: Format[TicketType] = new Format[TicketType] {
    override def reads(json: JsValue): JsResult[TicketType] = json match {
      case JsString(str) => JsSuccess(TicketType(str))
      case _ => JsError("Invalid type")
    }

    override def writes(o: TicketType): JsValue = JsString(TicketType.unapply(o))
  }
}