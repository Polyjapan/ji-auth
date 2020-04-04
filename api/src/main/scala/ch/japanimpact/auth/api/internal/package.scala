package ch.japanimpact.auth.api

import play.api.libs.json.{Format, Json}

/**
  * @author Louis Vialar
  */
package object internal {
  implicit val tokenResponseMapper: Format[TokenResponse] = Json.format[TokenResponse]

  case class TokenResponse(accessToken: String, refreshToken: String, duration: Int)
}
