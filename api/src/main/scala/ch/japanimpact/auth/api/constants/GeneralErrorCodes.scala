package ch.japanimpact.auth.api.constants

import play.api.libs.json.{Format, Json}

/**
  * @author Louis Vialar
  */
object GeneralErrorCodes {
  private val GeneralErrorCodeSelector = "*"

  /**
    * The json returned by the server in case of error
    */
  class RequestError(val errorCode: Int)

  private object RequestError {
    def apply(errorCode: Int): RequestError = new RequestError(errorCode)

    def unapply(arg: RequestError): Option[Int] = Some(arg.errorCode)
  }

  class ErrorCode(errorCode: Int, val endpoints: String*) extends RequestError(errorCode)

  class GeneralErrorCode(code: Int) extends ErrorCode(code, GeneralErrorCodeSelector)

  object ErrorCode {
    private val errors = List(UnknownError, MissingData, UnknownApp, InvalidAppSecret, InvalidCaptcha, InvalidTicket, GroupNotFound, MissingPermission)

    private val register: Map[String, Map[Int, ErrorCode]] = errors
      .flatMap(err => err.endpoints.map(endpoint => (endpoint, err)))
      .groupBy(_._1)
      .mapValues(_.map(e => (e._2.errorCode, e._2)).toMap).toMap

    def apply(code: Int, endpoint: String): ErrorCode = {
      val map = register(endpoint) ++ register(GeneralErrorCodeSelector)

      map.withDefaultValue(UnknownError)(code)
    }
  }

  case object UnknownError extends GeneralErrorCode(100)

  /**
    * Some data in the request is missing or invalid
    */
  case object MissingData extends GeneralErrorCode(101)

  /**
    * The requested app was not found
    */
  case object UnknownApp extends GeneralErrorCode(102)

  /**
    * The requested app was not found or the App Secret was incorrect
    */
  case object InvalidAppSecret extends GeneralErrorCode(103)

  /**
    * The captcha is invalid
    */
  case object InvalidCaptcha extends GeneralErrorCode(104)

  case object InvalidTicket extends ErrorCode(201, "get_ticket")

  case object GroupNotFound extends ErrorCode(201, "group_remove_user", "group_add_user", "group_get_users")

  case object UserNotFound extends ErrorCode(202, "group_remove_user", "group_add_user", "group_get_users")

  case object MissingPermission extends ErrorCode(203, "group_remove_user", "group_add_user", "group_get_users")


  implicit val errorFormat: Format[RequestError] = Json.format[RequestError]
}
