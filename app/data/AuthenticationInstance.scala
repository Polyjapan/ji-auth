package data

import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

object AuthenticationInstance {
  val SessionKey = "ai";


  private implicit val casInstanceFormat = Json.format[CASInstance]
  private implicit val tokensInstanceFormat = Json.format[TokensInstance]
  implicit val authenticationInstanceFormat = Json.format[AuthenticationInstance]


  def unapply(instance: AuthenticationInstance): Option[(String, JsValue)] = {
    val (prod: Product, sub) = instance match {
      case b: CASInstance => (b, Json.toJson(b)(casInstanceFormat))
      case b: TokensInstance => (b, Json.toJson(b)(tokensInstanceFormat))
    }
    Some(prod.productPrefix -> sub)
  }

  def apply(instanceType: String, constraint: JsValue): AuthenticationInstance = {
    (instanceType match {
      case "CASInstance" => Json.fromJson[CASInstance](constraint)
      case "TokensInstance" => Json.fromJson[TokensInstance](constraint)
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

case class CASInstance(url: String, serviceId: Int) extends AuthenticationInstance

case class TokensInstance(redirectUrl: String) extends AuthenticationInstance

