package ch.japanimpact.auth

import java.util.Date

import play.api.libs.json.{Format, Json}

/**
 * @author Louis Vialar
 */
package object api {
  implicit val addressMapper: Format[UserAddress] = Json.format[UserAddress]
  implicit val detailsMapper: Format[UserDetails] = Json.format[UserDetails]
  implicit val profileMapper: Format[UserProfile] = Json.format[UserProfile]
  implicit val dataMapper: Format[UserData] = Json.format[UserData]
  implicit val tokenResponseMapper: Format[TokenResponse] = Json.format[TokenResponse]
  implicit val groupMapper: Format[Group] = Json.format[Group]
  implicit val groupDataMapper: Format[GroupData] = Json.format[GroupData]

  case class AuthorizedUser(userId: Int, groups: Set[String])

  case class TokenResponse(accessToken: String, refreshToken: String, duration: Int)

  case class UserAddress(address: String, addressComplement: Option[String], postCode: String, city: String, country: String)

  case class UserDetails(firstName: String, lastName: String, phoneNumber: Option[String])

  case class UserProfile(id: Int, email: String, details: UserDetails, address: Option[UserAddress])

  case class UserData(id: Option[Int], email: String, emailConfirmed: Boolean, details: UserDetails,
                      passwordAlgo: String, passwordReset: Boolean, passwordResetEnd: Option[Date],
                      address: Option[UserAddress], scopes: Set[String], groups: Set[String])


  /**
   * Represents a group of users
   *
   * @param id          the id of the group
   * @param name        the internal name for the group ([a-zA-Z0-9_-]+)
   * @param displayName the display name for the group
   */
  case class Group(id: Option[Int], name: String, displayName: String)

  case class GroupData(group: Group, allowedScopes: Set[String])
}
