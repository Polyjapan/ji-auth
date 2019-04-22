package controllers

import data.Client
import javax.inject.Inject
import models.{ClientsModel, HashModel, TokensModel}
import play.api.Configuration
import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import utils.Implicits._
import constants.GeneralErrorCodes._

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class LoginController @Inject()(
                                 cc: ControllerComponents,
                                 clients: ClientsModel,
                                 tokens: TokensModel,
                                 hash: HashModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {

  val UserOrPasswordInvalid = 201
  val EmailNotConfirmed = 202

  private val randomHashed = hash("this is a random string")

  case class LoginRequest(username: String, password: String, clientId: String)

  case class LoginSuccess(refreshToken: String)

  case class LoginFailure(errorCode: Int)

  implicit val requestReads: Reads[LoginRequest] = Json.reads[LoginRequest]
  implicit val successWrites: Writes[LoginSuccess] = Json.writes[LoginSuccess]
  implicit val failureWrites: Writes[LoginFailure] = Json.writes[LoginFailure]

  private def fakeCheck(pass: String) = {
    hash.check(hash.DefaultAlgo, randomHashed, pass) // Spend some time to avoid timing attacks
  }

  def postLogin: Action[LoginRequest] = Action.async(parse.json[LoginRequest]) { implicit rq: Request[LoginRequest] =>
    if (rq.hasBody) {
      val body = rq.body
      clients findClient body.username flatMap {

        // Valid client (and we extract values that will be useful)
        case Some((client@Client(Some(id), _, _, _, emailConfirmKey, password, passwordAlgo, _, _, _), perms)) =>

          // Check if password is correct
          if (hash.check(passwordAlgo, password, body.password)) {

            // Check if email is confirmed
            if (emailConfirmKey.isEmpty) {

              // Try to upgrade password if needed
              val np = hash.upgrade(passwordAlgo, body.password)
              np match {
                case Some((algo, pass)) =>
                  // The method returned a new (algo, pass) pair ==> we have to update!
                  clients.updateClient(client.copy(passwordAlgo = algo, password = pass))

                case _ => // do nothing
              }

              // Create the token and return it
              val ua = rq.headers("User-Agent")
              val ip = rq.remoteAddress
              tokens createTokenForUser(id, ua, ip) map (token => Ok(Json.toJson(LoginSuccess(token))))
            } else {
              BadRequest(Json.toJson(LoginFailure(EmailNotConfirmed)))
            }
          } else BadRequest(Json.toJson(LoginFailure(UserOrPasswordInvalid)))


        case None =>
          // No account found... we just spend some time computing a fake password and return
          fakeCheck(body.password)
          BadRequest(Json.toJson(LoginFailure(UserOrPasswordInvalid)))
      }
    } else BadRequest(Json.toJson(LoginFailure(MissingData))) // No body or body parse fail ==> invalid input
  }


}
