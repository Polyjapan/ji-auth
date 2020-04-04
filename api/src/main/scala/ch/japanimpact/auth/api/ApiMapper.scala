package ch.japanimpact.auth.api

import ch.japanimpact.auth.api.constants.GeneralErrorCodes
import ch.japanimpact.auth.api.constants.GeneralErrorCodes.{ErrorCode, RequestError}
import play.api.libs.json.Reads
import play.api.libs.ws.WSResponse

import scala.util.Try

protected abstract class ApiMapper {

  protected def mapResponse[ReturnType](endpoint: String, produceSuccess: WSResponse => ReturnType, produceError: ErrorCode => ReturnType)(r: WSResponse) = {
    try {
      if (r.status != 200)
        produceError(Try { ErrorCode(r.json.as[RequestError].errorCode, endpoint) }.getOrElse { GeneralErrorCodes.UnknownError } )
      else produceSuccess(r)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        produceError(GeneralErrorCodes.UnknownError)
    }
  }

  protected def mapResponseToEither[SuccessType](endpoint: String)(implicit format: Reads[SuccessType]): WSResponse => Either[SuccessType, ErrorCode] = {
    mapResponse[Either[SuccessType, ErrorCode]](endpoint, r => Left(r.json.as[SuccessType]), e => Right(e))
  }
}
