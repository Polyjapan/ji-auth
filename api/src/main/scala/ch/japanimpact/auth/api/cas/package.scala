package ch.japanimpact.auth.api

import play.api.libs.json._


/**
  * @author Louis Vialar
  */
package object cas {

  /**
    * An error returned by the CAS server
    *
    * @param errorType the type of the error
    * @param message   the message returned with the error
    */
  case class CASError(errorType: CASErrorType, message: String)

  implicit val errorFormat = new OFormat[CASError] {
    override def reads(err: JsValue): JsResult[CASError] = {
      JsSuccess(CASError(CASErrorType((err \ "code").asOpt[String].getOrElse("undefined")), (err \ "description").asOpt[String].getOrElse("undefined")))
    }

    override def writes(o: CASError): JsObject =
      Json.obj("code" -> o.errorType.name, "description" -> o.errorType.message(o.message))
  }

  /**
    * User data returned by the CAS
    *
    * @param user                the user id
    * @param proxyGrantingTicket an optional PGT or PGTIOU returned by the server
    * @param attributes          user defined attributes, related to the user
    */
  case class CASServiceResponse(user: String, proxyGrantingTicket: Option[String], attributes: JsValue) {
    /**
      * Get the PGT contained in this request.
      * @return the PGT, or None if this request contains not PGT, or if the PGT-IOU to PGT mapping was not found
      */
    def PGT: Option[ProxyGrantingTicket] =
      this.proxyGrantingTicket.filter(CASTicketType.ProxyGrantingTicket.isInstance)

    /**
      * Get the `groups` attribute if present
      */
    def groups: Set[String] =
      (attributes \ "groups").asOpt[Set[String]].getOrElse(Set.empty)

    def attr(key: String): Option[String] = (attributes \ key).asOpt[String]
    def email: Option[String] = attr("email")
    def name: Option[String] = attr("name")
    def firstname: Option[String] = attr("firstname")
    def lastname: Option[String] = attr("lastname")
  }

  implicit val responseFormat = new Reads[CASServiceResponse] {
    override def reads(resp: JsValue): JsResult[CASServiceResponse] = {
      (resp \ "user").asOpt[String] match {
        case Some(user) =>
          val proxy = (resp \ "proxyGrantingTicket").asOpt[String]
          val attrs = (resp \ "attributes").getOrElse(Json.obj())

          JsSuccess(CASServiceResponse(user, proxy, attrs))
        case None => JsError("user not defined")
      }
    }
  }

  private def tryAsError[A](apiResult: JsValue)(noErr: JsValue => JsResult[A]): JsResult[Either[CASError, A]] = {
    val res = apiResult \ "serviceResponse"

    res \ "authenticationFailure" match {
      case JsDefined(fail) => errorFormat.reads(fail).map(e => Left(e))
      case _ => noErr(res.get).map(s => Right(s))
    }
  }

  implicit val casValidateFormat = new Reads[Either[CASError, CASServiceResponse]] {
    override def reads(json: JsValue): JsResult[Either[CASError, CASServiceResponse]] = tryAsError(json) {
      json =>
        json \ "authenticationSuccess" match {
          case JsDefined(data) => responseFormat.reads(data)
          case _ => JsError("authenticationSuccess")
        }
    }
  }

  implicit val casProxyFormat = new Reads[Either[CASError, ProxyTicket]] {
    override def reads(json: JsValue): JsResult[Either[CASError, ProxyTicket]] = tryAsError(json) {
      json =>
        json \ "proxySuccess" \ "proxyTicket" match {
          case JsDefined(data) => JsSuccess(data.as[ProxyTicket])
          case _ => JsError("proxySuccess.proxyTicket")
        }
    }
  }

  type ServiceTicket = String
  type ProxyTicket = String
  type ProxyGrantingTicket = String
  type PGT_IOU = String
}
