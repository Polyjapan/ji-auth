package ch.japanimpact.auth

import play.api.libs.json.{Format, Json}

/**
  * @author Louis Vialar
  */
package object api {
  implicit val addressMapper: Format[UserAddress] = Json.format[UserAddress]
  implicit val detailsMapper: Format[UserDetails] = Json.format[UserDetails]
  implicit val profileMapper: Format[UserProfile] = Json.format[UserProfile]
  implicit val tokenResponseMapper: Format[TokenResponse] = Json.format[TokenResponse]

  case class AuthorizedUser(userId: Int, groups: Set[String])

  case class TokenResponse(accessToken: String, refreshToken: String, duration: Int)

  case class UserAddress(address: String, addressComplement: Option[String], postCode: String, city: String, country: String)

  case class UserDetails(firstName: String, lastName: String, phoneNumber: Option[String])

  case class UserProfile(id: Int, email: String, details: UserDetails, address: Option[UserAddress])
}
