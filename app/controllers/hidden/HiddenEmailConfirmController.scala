package controllers.hidden

import ch.japanimpact.auth.api.TicketType
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import ch.japanimpact.auth.api.LoginSuccess
import javax.inject.Inject
import models.{AppsModel, TicketsModel, UsersModel}
import play.api.libs.json.{Json, Reads}
import play.api.mvc._
import utils.Implicits._

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
@deprecated("the hidden flow is deprecated, only the explicit one should be supported")
class HiddenEmailConfirmController @Inject()(
                                              cc: ControllerComponents,
                                              users: UsersModel,
                                              tickets: TicketsModel,
                                              apps: AppsModel)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
    * The email confirm code is invalid
    */
  val InvalidConfirmCode = 201

  /**
    * The format of the request sent by the client
    *
    * @param email    the email of the user
    * @param code     the email confirmation code
    * @param clientId the clientId of the app
    */
  case class EmailConfirmRequest(email: String, code: String, clientId: String)

  implicit val format: Reads[EmailConfirmRequest] = Json.reads[EmailConfirmRequest]

  def postEmailConfirm: Action[EmailConfirmRequest] = Action.async(parse.json[EmailConfirmRequest]) { implicit rq =>
    if (rq.hasBody) {
      val body = rq.body

      // Get client first
      apps getApp body.clientId flatMap {
        case Some(app) =>
          users.confirmEmail(body.email, body.code).flatMap {
            case Some(user) =>
              tickets createTicketForUser(user.id.get, app.appId.get, TicketType.EmailConfirmTicket) map
                LoginSuccess.apply map toOkResult[LoginSuccess]
            case None => !InvalidConfirmCode
          }
        case None => !UnknownApp
      }
    } else !MissingData // No body or body parse fail ==> invalid input
  }
}
