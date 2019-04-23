package controllers.hidden

import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import ch.japanimpact.auth.api.TicketType
import data.RegisteredUser
import javax.inject.Inject
import models.{AppsModel, HashModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import utils.Implicits._

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class HiddenLogInController @Inject()(
                                       cc: ControllerComponents,
                                       users: UsersModel,
                                       tickets: TicketsModel,
                                       apps: AppsModel,
                                       hashes: HashModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {

  import hashes._

  /**
    * The user or the password sent by the client match no user
    */
  val UserOrPasswordInvalid = 201

  /**
    * The user is valid but the email was never confirmed
    */
  val EmailNotConfirmed = 202

  private val randomHashed = hash("this is a random string")._2

  /**
    * The format of the request sent by the client
    *
    * @param email    the email of the user to login
    * @param password the password of the user to login
    * @param clientId the clientId of the app trying to log the user in
    */
  case class LoginRequest(email: String, password: String, clientId: String)

  /**
    * The object returned on a login success
    *
    * @param ticket the ticket the app can use to get the user data
    */
  case class LoginSuccess(ticket: String)

  implicit val requestReads: Reads[LoginRequest] = Json.reads[LoginRequest]
  implicit val successWrites: Writes[LoginSuccess] = Json.writes[LoginSuccess]

  /**
    * This method computes the bcrypt check of the provided password and compares it to a previously hashed constant value.
    * This method is just used to spend some time, to avoid leaking that an accounts doesn't exist because the API replies quicker.
    *
    * @param pass the password the user sent
    */
  private def fakeCheck(pass: String) = {
    check(DefaultAlgo, randomHashed, pass) // Spend some time to avoid timing attacks
  }

  def postLogin: Action[LoginRequest] = Action.async(parse.json[LoginRequest]) { implicit rq: Request[LoginRequest] =>
    if (rq.hasBody) {
      val body = rq.body

      // Get client first
      apps getApp body.clientId flatMap {
        case Some(app) =>
          users getUser body.email flatMap {
            case Some(user@RegisteredUser(Some(id), _, emailConfirmKey, password, passwordAlgo, _, _)) =>
              // Check if password is correct
              if (check(passwordAlgo, password, body.password)) {

                // Check if email is confirmed
                if (emailConfirmKey.isEmpty) {

                  // Try to upgrade password if needed
                  val np = upgrade(passwordAlgo, body.password)
                  np match {
                    case Some((algo, pass)) =>
                      // The method returned a new (algo, pass) pair ==> we have to update!
                      users updateUser user.copy(passwordAlgo = algo, password = pass)

                    case _ => // do nothing
                  }

                  // Create the token and return it
                  tickets createTicketForUser(id, app.id.get, TicketType.LoginTicket) map LoginSuccess map toOkResult[LoginSuccess]
                } else ! EmailNotConfirmed

              } else ! UserOrPasswordInvalid


            case None =>
              // No account found... we just spend some time computing a fake password and return
              fakeCheck(body.password)
              ! UserOrPasswordInvalid
          }

        case None => ! UnknownApp // No body or body parse fail ==> invalid input
      }
    } else ! MissingData // No body or body parse fail ==> invalid input
  }


}
