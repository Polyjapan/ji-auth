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

  implicit val requestMapper: Format[AppTicketRequest] = Json.format[AppTicketRequest]
  implicit val responseMapper: Format[AppTicketResponse] = Json.format[AppTicketResponse]
  implicit val successMapper: Format[LoginSuccess] = Json.format[LoginSuccess]

  /**
    * The format of the request sent by the client
    *
    * @param ticket       the ticket the CAS previously sent to the user
    * @param clientId     the clientId of the requesting app
    * @param clientSecret the clientSecret of the requesting app
    */
  @deprecated
  case class AppTicketRequest(ticket: String, clientId: String, clientSecret: String)

  case class AuthorizedUser(userId: Int, groups: Set[String])

  case class TokenResponse(accessToken: String, refreshToken: String, duration: Int)

  /**
    * The object returned when the ticket is found
    *
    * @param userId     the CAS id of user the ticket was generated for
    * @param userEmail  the email of the user the ticket was generated for
    * @param ticketType the type of ticket
    * @param groups     the set of groups (exposed to the app) the user is part of
    */
  @deprecated
  case class AppTicketResponse(@deprecated userId: Int, @deprecated userEmail: String, ticketType: TicketType, groups: Set[String], user: UserProfile)

  case class UserAddress(address: String, addressComplement: Option[String], postCode: String, city: String, country: String)

  case class UserDetails(firstName: String, lastName: String, phoneNumber: Option[String])

  case class UserProfile(id: Int, email: String, details: UserDetails, address: Option[UserAddress])

  /**
    * The object returned when the login is successful
    *
    * @param ticket the returned ticket
    */
  @deprecated
  case class LoginSuccess(ticket: String)

}
