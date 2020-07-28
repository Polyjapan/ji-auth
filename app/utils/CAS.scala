package utils

import ch.japanimpact.auth.api.cas.{CASErrorType, StringHelper}
import play.api.http.Writeable
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{ActionBuilder, ControllerComponents, Result, Results}

import scala.xml._

object CAS {
  trait CASResponses {
    def getErrorResponse(errType: CASErrorType, param: String): Result

    def getProxyErrorResponse(errType: CASErrorType, param: String): Result

    def getProxySuccessResponse(ticket: String): Result

    def getSuccessResponse(properties: Map[String, String], attributes: Map[String, String], groups: Set[String]): Result
  }

  object XMLCasResponses extends CASResponses {
    private val ctor: Elem => Result = e => Results.Ok(e)

    override def getErrorResponse(errType: CASErrorType, param: String): Result = ctor {
      <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
        <cas:authenticationFailure code={errType.name}>{errType.message(param)}</cas:authenticationFailure>
      </cas:serviceResponse>
    }

    override def getProxyErrorResponse(errType: CASErrorType, param: String): Result = ctor {
      <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
        <cas:proxyFailure code={errType.name}>{errType.message(param)}</cas:proxyFailure>
      </cas:serviceResponse>
    }

    override def getProxySuccessResponse(ticket: String): Result = ctor {
      <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
        <cas:proxySuccess>
          <cas:proxyTicket>{ticket}</cas:proxyTicket>
        </cas:proxySuccess>
      </cas:serviceResponse>
    }

    override def getSuccessResponse(properties: Map[String, String], attributes: Map[String, String], groups: Set[String]): Result = ctor {
      <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
        <cas:authenticationSuccess>
          {properties.map { case (k, v) => Elem("cas", k, Null, TopScope, false, Text(v)) }}<cas:attributes>
          {attributes.map { case (k, v) => Elem("cas", k, Null, TopScope, false, Text(v)) }}{groups.map(g => Elem("cas", "groups", Null, TopScope, false, Text(g)))}
        </cas:attributes>
        </cas:authenticationSuccess>
      </cas:serviceResponse>
    }
  }

  object JSONCasResponses extends CASResponses {
    private val ctor: JsObject => Result = e => Results.Ok(e)

    override def getErrorResponse(errType: CASErrorType, param: String) = ctor {
      Json.obj("serviceResponse" ->
        Json.obj("authenticationFailure" -> Json.obj(
          "code" -> errType.name, "description" -> errType.name
        ))
      )
    }

    override def getProxySuccessResponse(ticket: String) = ctor {
      Json.obj("serviceResponse" ->
        Json.obj("proxySuccess" -> Json.obj(
          "proxyTicket" -> ticket
        ))
      )
    }

    override def getSuccessResponse(properties: Map[String, String], attributes: Map[String, String], groups: Set[String]) = ctor {
      val attributesMap = Json.toJsObject(attributes) + ("groups" -> Json.toJson(groups))

      Json.obj("serviceResponse" ->
        Json.obj("authenticationSuccess" -> (Json.toJsObject(properties) + ("attributes" -> attributesMap)))
      )
    }

    override def getProxyErrorResponse(errType: CASErrorType, param: String) =
      getErrorResponse(errType, param)
  }

  def getServiceDomain(service: String): Option[String] =
    StringHelper.getServiceDomain(service)



}
