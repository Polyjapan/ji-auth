package controllers.saml2


import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import scala.xml.NodeSeq

object SAMLv2Parser {
  def parseNameIDPolicy(policy: xml.Elem): NameIDPolicy = {
    val format = (policy attribute "Format").map(_.text)
    val allowCreate = (policy attribute "AllowCreate").map(_.text == "true")

    NameIDPolicy(format, allowCreate)
  }

  def parseAuthnRequest(request: xml.Elem): SAMLv2AuthnRequest = {
    val id = request \@ "ID"
    val version = request \@ "Version"
    val instant = request \@ "IssueInstant"

    val dest: Option[String] = request.attribute("Destination").map(_.text)
    val issuer: Option[String] = (request \ "Issuer").headOption.map(_.text)

    val nameIDPolicy: Option[NameIDPolicy] = (request \ "NameIDPolicy").collectFirst { case elem: xml.Elem =>parseNameIDPolicy(elem) }

    val forceAuthn = request.attribute("ForceAuthn").exists(_.text == "true")
    val isPassive = request.attribute("IsPassive").exists(_.text == "true")

    val assertionConsumerServiceURL = request.attribute("AssertionConsumerServiceURL").map(_.text)
    val protocolBinding = request.attribute("ProtocolBinding").map(_.text)

    SAMLv2AuthnRequest(id, version, Instant.parse(instant), dest, issuer, nameIDPolicy, forceAuthn, isPassive, assertionConsumerServiceURL, protocolBinding)
  }

  @throws[IllegalVersionException]
  @throws[InvalidRequestException]
  def apply(x: NodeSeq): SAMLv2Request = {
    val elems = x.filter { case x: xml.Elem => true case _ => false }.map { case x: xml.Elem => x }

    elems.collectFirst {
      case x if x.label == "AuthnRequest" => parseAuthnRequest(x)
      case _ => throw InvalidRequestException()
    } match {
      case Some(rq) if rq.version == "2.0" => rq
      case Some(_) => throw IllegalVersionException()
      case None => throw InvalidRequestException()
    }
  }

  /**
   * A basic SAMLv2 request
   */
  trait SAMLv2Request {
    /**
     * An identifier for the request, of type xs:ID, unique
     */
    val requestId: String
    /**
     * The version of the protocol, should be 2.0
     */
    val version: String
    /**
     * The instant the request was issued
     */
    val issueInstant: Instant

    /**
     * The URI to which the request has been sent. If present, must be checked (prevents malicious relaying)
     */
    val destination: Option[String]

    /**
     * Identifies the entity that generated the message
     */
    val issuer: Option[String]
  }

  /**
   * @param nameIdPolicy
   * @param forceAuthn                  if true, the user must log in again (no previous SSO session)
   * @param passive                     if true, the user should not see any login (transparent redirect) -- takes precedence over forceAuthn
   * @param assertionConsumerServiceURL if present, the response MUST be returned to this URL
   */
  case class SAMLv2AuthnRequest(
                                 requestId: String,
                                 version: String,
                                 issueInstant: Instant,
                                 destination: Option[String],
                                 issuer: Option[String],
                                 nameIdPolicy: Option[NameIDPolicy],
                                 forceAuthn: Boolean = false,
                                 passive: Boolean = false,
                                 assertionConsumerServiceURL: Option[String],
                                 protocolBinding: Option[String]
                               ) extends SAMLv2Request

  /**
   * 3.4.1.1 Element <NameIdPolicy>
   *
   * @param format      URI reference corresponding to the name identifier format defined in a spec'
   * @param allowCreate in standard defaults to false, basically says whether the user is allowed to register
   */
  case class NameIDPolicy(format: Option[String], allowCreate: Option[Boolean])

  case class SamlValidateRequest(requestId: String, timestamp: Instant, serviceTicket: String)

  case class IllegalVersionException() extends Exception

  case class InvalidRequestException() extends Exception
}
