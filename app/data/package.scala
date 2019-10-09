import java.sql.Timestamp

import ch.japanimpact.auth.api.{TicketType, UserAddress, UserDetails}

/**
  * @author Louis Vialar
  */
package object data {

  /**
    * Defines a user registered against the server. (In the future, might support sign on via Google/Twitter/Tequila...)
    *
    * @param id               the id of the client in database
    * @param email            the email of the client
    * @param emailConfirmKey  an optional confirmation key for the email. If it's null then the email has been validated
    * @param password         the hashed password of the user
    * @param passwordAlgo     the algorithm used to hash the password
    * @param passwordReset    an optional reset key for the password. If it's null then no password change was requested
    * @param passwordResetEnd an optional timestamp marking the date at which the password reset key will no longer be valid
    *                         If absent, the password reset key is considered invalid
    * @param adminLevel       the admin level (1-10)
    */
  case class RegisteredUser(id: Option[Int], email: String, emailConfirmKey: Option[String], password: String,
                            passwordAlgo: String, passwordReset: Option[String] = Option.empty,
                            passwordResetEnd: Option[Timestamp] = Option.empty,
                            adminLevel: Int = 0,
                            firstName: String,
                            lastName: String,
                            phoneNumber: Option[String] = None
                           ) {

    def toUserDetails = UserDetails(firstName, lastName, phoneNumber)
  }

  case class Address(userId: Int, address: String, addressComplement: Option[String], postCode: String, city: String, country: String) {
    def toUserAddress = UserAddress(address, addressComplement, postCode, city, country)
  }

  /**
    * Represents an app allowed to authenticate users in the system
    *
    * @param id               an internal id for the app
    * @param createdBy        the id of the user who created the app
    * @param clientId         the clientID to provide in requests
    * @param clientSecret     the client secret to authenticate secret (backend to backend) requests
    * @param name             the name of the app
    * @param redirectUrl      the URL where the user should be redirected after logging in
    * @param emailRedirectUrl the URL that should be embedded in email confirmation emails sent in hidden mode
    * @param recaptchaPrivate the private key used by recaptcha, unique to the app
    */
  case class App(id: Option[Int], createdBy: Int, clientId: String, clientSecret: String, name: String, redirectUrl: String, emailRedirectUrl: String, recaptchaPrivate: Option[String])

  /**
    * Represents a ticket that is returned to the user by the CAS, and that the user has to "use" against the service it
    * was emitted for, in order to gain access to that service. That service will use this ticket to get some info about
    * the user (it will then be invalidated) as well as the type of action the ticket was emitted for, and then maybe
    * create a session for the user in the way prefered by the service.
    *
    * @param token      the unique token returned to the user
    * @param userId     the user this token bounds to
    * @param appId      the app this token was emitted for
    * @param validTo    the last timestamp this ticket can be used
    * @param ticketType the type of ticket (see [[TicketType]])
    */
  case class Ticket(token: String, userId: Int, appId: Int, validTo: Timestamp, ticketType: TicketType)

  /**
    * Represents a group of users
    *
    * @param id the id of the group
    * @param owner the current owner (user id) of the group
    * @param name the internal name for the group ([a-zA-Z0-9_-]+)
    * @param displayName the display name for the group
    */
  case class Group(id: Option[Int], owner: Int, name: String, displayName: String)

  /**
    * Represents a relationship between a group and its members
    * @param group the id of the group
    * @param user the id of the user
    * @param canManage if true, the user can add and remove other users (only non admins)
    * @param canRead if true, the user and its apps can read the list of members
    * @param isAdmin if true, the user can manage the users' rights
    */
  case class GroupMember(group: Int, user: Int, canManage: Boolean, canRead: Boolean, isAdmin: Boolean)
}
