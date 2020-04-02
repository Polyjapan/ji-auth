import java.sql.PreparedStatement
import java.util.{Base64, Date}

import anorm.Macro.ColumnNaming
import anorm.{Column, Macro, RowParser, ToParameterList, ToStatement}
import ch.japanimpact.auth.api.TicketType.{AppTicket, DoubleRegisterTicket, EmailConfirmTicket, ExplicitGrantTicket, LoginTicket, PasswordResetTicket, RegisterTicket}
import ch.japanimpact.auth.api.{TicketType, UserAddress, UserDetails}

/**
 * @author Louis Vialar
 */
package object data {

  case class SessionID(bytes: Array[Byte]) {
    override def toString: String = Base64.getUrlEncoder.encodeToString(bytes)
  }

  object SessionID {
    def apply(str: String): SessionID = {
      val btes = Base64.getUrlDecoder.decode(str)

      if (btes.length != 16) throw new IllegalArgumentException
      else SessionID(btes)
    }
  }

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
                            passwordResetEnd: Option[Date] = Option.empty,
                            adminLevel: Int = 0,
                            firstName: String,
                            lastName: String,
                            phoneNumber: Option[String] = None
                           ) {

    def toUserDetails = UserDetails(firstName, lastName, phoneNumber)
  }


  implicit val RegisteredUserRowParser: RowParser[RegisteredUser] = Macro.namedParser[RegisteredUser](ColumnNaming.SnakeCase)
  implicit val RegisteredUserParameterList: ToParameterList[RegisteredUser] = Macro.toParameters[RegisteredUser]()

  case class Address(userId: Int, address: String, addressComplement: Option[String], postCode: String, city: String, country: String) {
    def toUserAddress = UserAddress(address, addressComplement, postCode, city, country)
  }

  implicit val AddressRowParser: RowParser[Address] = Macro.namedParser[Address](ColumnNaming.SnakeCase)
  implicit val AddressParameterList: ToParameterList[Address] = Macro.toParameters[Address]()


  /**
   * Represents an app allowed to make API calls on the system
   *
   * @param appId               an internal id for the app
   * @param appCreatedBy        the id of the user who created the app
   * @param clientSecret        the client secret to authenticate secret (backend to backend) requests
   * @param appName             the name of the app
   */
  case class ApiKey(appId: Option[Int], appCreatedBy: Int, clientSecret: String, appName: String)

  case class CasService(serviceId: Int, serviceName: String, serviceRedirectUrl: Option[String] = None)
  case class CasV2Ticket(email: String, firstname: String, lastname: String, groups: Set[String])

  implicit val AppRowParser: RowParser[ApiKey] = Macro.namedParser[ApiKey](ColumnNaming.SnakeCase)
  implicit val AppParameterList: ToParameterList[ApiKey] = Macro.toParameters[ApiKey]()

  /**
   * Represents a group of users
   *
   * @param id          the id of the group
   * @param owner       the current owner (user id) of the group
   * @param name        the internal name for the group ([a-zA-Z0-9_-]+)
   * @param displayName the display name for the group
   */
  case class Group(id: Option[Int], owner: Int, name: String, displayName: String)

  implicit val GroupRowParser: RowParser[Group] = Macro.namedParser[Group](ColumnNaming.SnakeCase)
  implicit val GroupParameterList: ToParameterList[Group] = Macro.toParameters[Group]()

  /**
   * Represents a relationship between a group and its members
   *
   * @param groupId          the id of the group
   * @param userId           the id of the user
   * @param canManageMembers if true, the user can add and remove other users (only non admins)
   * @param canReadMembers   if true, the user and its apps can read the list of members
   * @param isAdmin          if true, the user can manage the users' rights
   */
  case class GroupMember(groupId: Int, userId: Int, canManageMembers: Boolean, canReadMembers: Boolean, isAdmin: Boolean)

  implicit val GroupMemberRowParser: RowParser[GroupMember] = Macro.namedParser[GroupMember](ColumnNaming.SnakeCase)
  implicit val GroupMemberParameterList: ToParameterList[GroupMember] = Macro.toParameters[GroupMember]()

}
