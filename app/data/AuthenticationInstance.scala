package data

import controllers.saml2.SAMLv2Parser.{NameIDPolicy, SAMLv2AuthnRequest}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, OFormat}

object AuthenticationInstance {
  val SessionKey = "ai"

  private implicit val casInstanceFormat: OFormat[CASInstance] = Json.format[CASInstance]
  private implicit val samlInstanceFormat: OFormat[SAMLv2Instance] = Json.format[SAMLv2Instance]
  implicit val authenticationInstanceFormat: OFormat[AuthenticationInstance] = Json.format[AuthenticationInstance]

  def unapply(instance: AuthenticationInstance): Option[(String, JsValue)] = {
    val (prod: Product, sub) = instance match {
      case b: CASInstance => (b, Json.toJson(b)(casInstanceFormat))
    }
    Some(prod.productPrefix -> sub)
  }

  def apply(instanceType: String, constraint: JsValue): AuthenticationInstance = {
    (instanceType match {
      case "CASInstance" => Json.fromJson[CASInstance](constraint)
      case other =>
        println(other)
        throw new IllegalArgumentException("No case for " + other)
    }) match {
      case JsSuccess(res, _) => res
      case JsError(errors) =>
        errors.foreach(println)
        throw new IllegalArgumentException("A JS error occurred")
    }
  }
}

sealed trait AuthenticationInstance {
  def pair: (String, String) = AuthenticationInstance.SessionKey -> Json.stringify(Json.toJson(this))
}

case class CASInstance(url: String, serviceId: Int, requireFullInfo: Boolean) extends AuthenticationInstance

case class SAMLv2Instance(url: String, relay: Option[String], binding: String,
                          requestId: String, nameIDFormat: String, serviceId: Int, requireFullInfo: Boolean) extends AuthenticationInstance



