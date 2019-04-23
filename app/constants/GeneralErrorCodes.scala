package constants

import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Result, Results}

/**
  * @author Louis Vialar
  */
object GeneralErrorCodes {
  /**
    * Some data in the request is missing or invalid
    */
  val MissingData = 101

  /**
    * The requested app was not found
    */
  val UnknownApp = 102

  /**
    * The requested app was not found or the App Secret was incorrect
    */
  val InvalidAppSecret = 103

  case class RequestError(errorCode: Int)

  implicit val failureWrites: Writes[RequestError] = Json.writes[RequestError]

  def error(code: Int): Result = Results.BadRequest(Json.toJson(RequestError(code)))
}
