package controllers.hidden

import java.util.Date

import ch.japanimpact.auth.api.TicketType
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import data.{LoginSuccess, RegisteredUser}
import javax.inject.Inject
import models.{AppsModel, HashModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import utils.Implicits._
import utils.ValidationUtils

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class HiddenPasswordResetController @Inject()(
                                               cc: ControllerComponents,
                                               users: UsersModel,
                                               tickets: TicketsModel,
                                               apps: AppsModel,
                                               hashes: HashModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {

  /**
    * The password reset code is invalid (wrong user or code)
    */
  val InvalidResetCode = 201

  val PasswordTooShort = 202


  /**
    * The format of the request sent by the client
    *
    * @param email    the email of the user
    * @param code     the password reset code
    * @param clientId the clientId of the app
    * @param password the new password of the user
    */
  case class PasswordResetRequest(email: String, code: String, clientId: String, password: String)

  implicit val format: Reads[PasswordResetRequest] = Json.reads[PasswordResetRequest]

  def postPasswordReset: Action[PasswordResetRequest] = Action.async(parse.json[PasswordResetRequest]) { implicit rq =>
    if (rq.hasBody) {
      val body = rq.body

      if (!ValidationUtils.isValidPassword(body.password)) {
        ! PasswordTooShort
      } else apps getApp body.clientId flatMap {
        case Some(app) =>
          users getUser body.email flatMap {
            case Some(user@RegisteredUser(Some(id), _, _, _, _, Some(code), Some(deadline), _)) if code == body.code && deadline.after(new Date) =>

              // Hash the new password and update the user
              val (algo, hashPass) = hashes hash body.password
              val updated = user.copy(password = hashPass, passwordAlgo = algo, passwordReset = None, passwordResetEnd = None)

              // Store the updated user and generate a ticket to return
              users updateUser updated flatMap { _ =>
                tickets.createTicketForUser(id, app.id.get, TicketType.PasswordResetTicket) map LoginSuccess.apply map toOkResult[LoginSuccess]
              }
            case _ => !InvalidResetCode
          }

        case None => !UnknownApp // No body or body parse fail ==> invalid input
      }
    } else !MissingData // No body or body parse fail ==> invalid input
  }


}
