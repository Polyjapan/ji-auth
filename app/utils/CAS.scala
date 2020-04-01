package utils

import scala.xml._

object CAS {
  private val domainRegex = "^[a-zA-Z0-9_.-]+$".r
  private val serviceRegex = "^https?://([a-zA-Z0-9_.-]+)".r

  def getServiceDomain(service: String) = {
    if (domainRegex.matches(service)) Some(service)
    else serviceRegex.findFirstMatchIn(service).map(r => r.group(1))
  }

  def getCasErrorResponse(errType: CASError.CASErrorType, param: String): Elem = {
    <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
      <cas:authenticationFailure code={errType.name}>
        {errType.message(param)}
      </cas:authenticationFailure>
    </cas:serviceResponse>
  }

  def getCasSuccessMessage(properties: Map[String, String]): Elem = {
    <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
      <cas:authenticationSuccess>
        {
          properties.map { case (k, v) => Elem(null, k, Null, TopScope, false, Text(v)) }
        }
      </cas:authenticationSuccess>
    </cas:serviceResponse>
  }

  object CASError {

    case class CASErrorType(name: String, message: String => String)

    val InvalidRequest = CASErrorType("INVALID_REQUEST", (s: String) => s"Missing parameters $s")
    val InvalidTicket = CASErrorType("INVALID_TICKET", (s: String) => s"Ticket $s not recognized")
    val InvalidService = CASErrorType("INVALID_SERVICE", (s: String) => s"Service $s not recognized")
    val InternalError = CASErrorType("INTERNAL_ERROR", (s: String) => s"An internal error occurred: $s")
  }

}
