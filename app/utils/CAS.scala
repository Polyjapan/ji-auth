package utils

import ch.japanimpact.auth.api.cas.{CASErrorType, StringHelper}
import play.api.libs.json.Json

import scala.xml._

object CAS {
  def getServiceDomain(service: String): Option[String] =
    StringHelper.getServiceDomain(service)


  def getCasErrorResponseXML(errType: CASErrorType, param: String): Elem = {
    <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
      <cas:authenticationFailure code={errType.name}>
        {errType.message(param)}
      </cas:authenticationFailure>
    </cas:serviceResponse>
  }

  def getCasErrorResponseJson(errType: CASErrorType, param: String) =
    Json.obj("serviceResponse" ->
      Json.obj("authenticationFailure" -> Json.obj(
        "code" -> errType.name, "description" -> errType.name
      ))
    )

  def getProxyErrorResponseXML(errType: CASErrorType, param: String): Elem = {
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


  def getCasSuccessMessageXML(properties: Map[String, String], attributes: Map[String, String], groups: Set[String]): Elem = {
    <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
      <cas:authenticationSuccess>
        {properties.map { case (k, v) => Elem("cas", k, Null, TopScope, false, Text(v)) }}<cas:attributes>
        {attributes.map { case (k, v) => Elem("cas", k, Null, TopScope, false, Text(v)) }}{groups.map(g => Elem("cas", "groups", Null, TopScope, false, Text(g)))}
      </cas:attributes>
      </cas:authenticationSuccess>
    </cas:serviceResponse>
  }

  def getCasSuccessMessageJson(properties: Map[String, String], attributes: Map[String, String], groups: Set[String]) = {
    val attributesMap = Json.toJsObject(attributes) + ("groups" -> Json.toJson(groups))

    Json.obj("serviceResponse" ->
      Json.obj("authenticationSuccess" -> (Json.toJsObject(properties) + ("attributes" -> attributesMap)))
    )
  }


}
