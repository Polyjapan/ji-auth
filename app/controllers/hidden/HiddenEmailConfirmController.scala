package controllers.hidden

import ch.japanimpact.auth.api.TicketType
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import data.{LoginSuccess, RegisteredUser}
import javax.inject.Inject
import models.{AppsModel, HashModel, TicketsModel, UsersModel}
import play.api.libs.json.{Json, Reads}
import play.api.mvc._
import utils.Implicits._

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
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

          // Then get the user by its email
          users getUser body.email flatMap {

            // Check if the user needs email confirmation and if the code is correct
            case Some(user@RegisteredUser(Some(id), _, Some(c), _, _, _, _)) if c == body.code =>

              // Update the user to mark the email is confirmed, and return a new ticket
              users updateUser user.copy(emailConfirmKey = None) flatMap { _ =>
                tickets createTicketForUser(id, app.id.get, TicketType.EmailConfirmTicket) map
                  LoginSuccess.apply map toOkResult[LoginSuccess]
              }
            case _ =>
              !InvalidConfirmCode
          }

        case None => !UnknownApp
      }
    } else !MissingData // No body or body parse fail ==> invalid input
  }
}
