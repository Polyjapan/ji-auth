package controllers.api

import ch.japanimpact.auth.api.apitokens._
import ch.japanimpact.auth.api.cas.CASService
import javax.inject.Inject
import models.{ApiKeysModel, UsersModel}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import services.JWTService
import utils.Implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * @author Louis Vialar
 */
class ApiTokensController @Inject()(cc: ControllerComponents, cas: CASService, jwt: JWTService, users: UsersModel)
                                   (implicit ec: ExecutionContext, apiKeys: ApiKeysModel, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {


  def userToken = Action.async(parse.json[UserTokenRequest]) { implicit req =>
    val request = req.body
    cas.proxyValidate(request.ticket, None) flatMap {
      case Right(resp) =>
        val userId = resp.user.toInt
        users.getAllowedScopes(userId) map { allowedScopes =>
          val scopes = request.scopes.filter(allowedScopes)

          val (dur, token) = jwt.issueApiToken(User(resp.user.toInt), scopes, request.audiences, request.duration.seconds)

          Ok(Json.toJson(TokenResponse(token, scopes, request.audiences, dur)))
        }
      case Left(err) =>
        println("CAS Error: " + err)
        Unauthorized(Json.toJson(ErrorResponse(s"cas:${err.errorType.name}", err.message))).asFuture
    }
  }

  def appToken = Action.async(parse.json[AppTokenRequest]) { implicit req =>
    ApiUtils
      .withApp(errorToReturn = Json.toJson(ErrorResponse("unauthorized", "the api key is not valid or missing"))) { app =>
        val request = req.body
        apiKeys.getAllowedScopes(app.appId.get) map { allowedScopes =>
          val scopes = request.scopes.filter(allowedScopes)
          val (dur, token) = jwt.issueApiToken(App(app.appId.get), scopes, request.audiences, request.duration.seconds)

          Ok(Json.toJson(TokenResponse(token, scopes, request.audiences, dur)))
        }
      }
  }

}
