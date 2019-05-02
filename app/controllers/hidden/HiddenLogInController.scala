package controllers.hidden

import ch.japanimpact.auth.api.TicketType
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import ch.japanimpact.auth.api.LoginSuccess
import javax.inject.Inject
import models.{AppsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import play.api.mvc._
import services.HashService
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
                                       hashes: HashService)(implicit ec: ExecutionContext, config: Configuration) extends AbstractController(cc) {

  /**
    * The user or the password sent by the client match no user
    */
  val UserOrPasswordInvalid = 201

  /**
    * The user is valid but the email was never confirmed
    */
  val EmailNotConfirmed = 202

  /**
    * The format of the request sent by the client
    *
    * @param email    the email of the user to login
    * @param password the password of the user to login
    * @param clientId the clientId of the app trying to log the user in
    */
  case class LoginRequest(email: String, password: String, clientId: String)


  implicit val requestReads: Reads[LoginRequest] = Json.reads[LoginRequest]


  def postLogin: Action[LoginRequest] = Action.async(parse.json[LoginRequest]) { implicit rq: Request[LoginRequest] =>
    if (rq.hasBody) {
      val body = rq.body

      apps getApp body.clientId flatMap {
        case Some(app) =>
          users.login(body.email, body.password).flatMap {
            case users.BadLogin => !UserOrPasswordInvalid
            case users.EmailNotConfirmed => !EmailNotConfirmed
            case users.LoginSuccess(user) =>
              tickets createTicketForUser(user.id.get, app.id.get, TicketType.LoginTicket) map LoginSuccess.apply map toOkResult[LoginSuccess]
          }
        case None => !UnknownApp // No body or body parse fail ==> invalid input
      }
    } else !MissingData // No body or body parse fail ==> invalid input
  }


}
