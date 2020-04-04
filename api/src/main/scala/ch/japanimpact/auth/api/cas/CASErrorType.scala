package ch.japanimpact.auth.api.cas

import scala.collection.mutable

sealed abstract class CASErrorType(val name: String, val message: String => String) {
  CASErrorType.register(this)
}

object CASErrorType {
  private val Map = mutable.Map[String, CASErrorType]();

  private def register(errorType: CASErrorType): Unit = {
    Map.put(errorType.name, errorType)
  }

  def apply(code: String): CASErrorType = Map.getOrElse(code, UnknownError(code))

  case object InvalidRequest extends CASErrorType("INVALID_REQUEST", (s: String) => s"Missing parameters $s")
  case object InvalidTicket extends CASErrorType("INVALID_TICKET", (s: String) => s"Ticket $s not recognized")
  case object InvalidTicketSpec extends CASErrorType("INVALID_TICKET_SPEC", (s: String) => s"Ticket $s not valid")
  case object UnauthorizedServiceProxy extends CASErrorType("UNAUTHORIZED_SERVICE_PROXY", (s: String) => s"Service $s is not authorized for proxy")
  case object InvalidProxyCallback extends CASErrorType("INVALID_PROXY_CALLBACK", (s: String) => s"Proxy callback $s is invalid")
  case object InvalidService extends CASErrorType("INVALID_SERVICE", (s: String) => s"Service $s not recognized")
  case object InternalError extends CASErrorType("INTERNAL_ERROR", (s: String) => s"An internal error occurred: $s")

  case class UnknownError(errorType: String) extends CASErrorType(errorType, s => "Unknown error - " + s)
}