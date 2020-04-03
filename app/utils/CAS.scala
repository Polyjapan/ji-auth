package utils

import play.api.libs.json.Json

import scala.xml._

object CAS {
  private val domainRegex = "^[a-zA-Z0-9_.-]+$".r
  private val serviceRegex = "^[a-z]{4,10}://([a-zA-Z0-9_.-]+)".r

  def getServiceDomain(service: String) = {
    if (domainRegex.matches(service)) Some(service)
    else serviceRegex.findFirstMatchIn(service).map(r => r.group(1))
  }

  def getCasErrorResponseXML(errType: CASError.CASErrorType, param: String): Elem = {
    <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
      <cas:authenticationFailure code={errType.name}>
        {errType.message(param)}
      </cas:authenticationFailure>
    </cas:serviceResponse>
  }

  def getCasErrorResponseJson(errType: CASError.CASErrorType, param: String) =
    Json.obj("serviceResponse" ->
      Json.obj("authenticationFailure" -> Json.obj(
        "code" -> errType.name, "description" -> errType.name
      ))
    )

  def getProxyErrorResponseXML(errType: CASError.CASErrorType, param: String): Elem = {
    <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
      <cas:proxyFailure code={errType.name}>
        {errType.message(param)}
      </cas:proxyFailure>
    </cas:serviceResponse>
  }

  def getProxySuccessXML(ticket: String): Elem = {
    <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
      <cas:proxySuccess>
        <cas:proxyTicket>
          {ticket}
        </cas:proxyTicket>
      </cas:proxySuccess>
    </cas:serviceResponse>
  }

  def getProxySuccessJson(ticket: String) =
    Json.obj("serviceResponse" ->
      Json.obj("proxySuccess" -> Json.obj(
        "proxyTicket" -> ticket
      ))
    )


  def getCasSuccessMessageXML(properties: Map[String, String], attributes: Map[String, String], username: String, groups: Set[String]): Elem = {
    <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
      <cas:authenticationSuccess>
        <cas:user>
          {username}
        </cas:user>{properties.map { case (k, v) => Elem("cas", k, Null, TopScope, false, Text(v)) }}<cas:attributes>
        {attributes.map { case (k, v) => Elem("cas", k, Null, TopScope, false, Text(v)) }}{groups.map(g => Elem("cas", "groups", Null, TopScope, false, Text(g)))}
      </cas:attributes>
      </cas:authenticationSuccess>
    </cas:serviceResponse>
  }

  def getCasSuccessMessageJson(properties: Map[String, String], attributes: Map[String, String], username: String, groups: Set[String]) = {
    val contentMap = Json.toJsObject(properties.updated("user", username))
    val attributesMap = Json.toJsObject(attributes) + ("groups" -> Json.toJson(groups))

    Json.obj("serviceResponse" ->
      Json.obj("authenticationSuccess" -> (contentMap + ("attributes" -> attributesMap)))
    )
  }

  object CASError {

    case class CASErrorType(name: String, message: String => String)

    val InvalidRequest = CASErrorType("INVALID_REQUEST", (s: String) => s"Missing parameters $s")
    val InvalidTicket = CASErrorType("INVALID_TICKET", (s: String) => s"Ticket $s not recognized")
    val InvalidTicketSpec = CASErrorType("INVALID_TICKET_SPEC", (s: String) => s"Ticket $s not valid")
    val UnauthorizedServiceProxy = CASErrorType("UNAUTHORIZED_SERVICE_PROXY", (s: String) => s"Service $s is not authorized for proxy")
    val InvalidProxyCallback = CASErrorType("INVALID_PROXY_CALLBACK", (s: String) => s"Proxy callback $s is invalid")
    val InvalidService = CASErrorType("INVALID_SERVICE", (s: String) => s"Service $s not recognized")
    val InternalError = CASErrorType("INTERNAL_ERROR", (s: String) => s"An internal error occurred: $s")
  }

}
