import java.util.{Base64, Date}
import anorm.Macro.ColumnNaming
import anorm.{Macro, RowParser, ToParameterList}
import ch.japanimpact.auth.api.{Group, UserAddress, UserDetails, UserProfile}
import com.yubico.webauthn.data.ByteArray
import play.api.libs.json.{Format, JsObject, Json, Writes}

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
   * @param adminLevel       the admin level (1-10) [Unused]
   */
  case class RegisteredUser(id: Option[Int] = None, email: String,
                            emailConfirmKey: Option[String] = None,
                            emailConfirmLastSent: Option[Date] = None,
                            password: String,
                            passwordAlgo: String,
                            passwordReset: Option[String] = None,
                            passwordResetEnd: Option[Date] = None,
                            adminLevel: Int = 0,
                            firstName: String,
                            lastName: String,
                            phoneNumber: Option[String] = None,
                            newsletter: Boolean,
                            userHandle: Option[String] = None
                           ) {

    def toUserDetails = UserDetails(firstName, lastName, phoneNumber)

    def toUserProfile(address: Option[UserAddress] = None) =
      UserProfile(id.get, email, toUserDetails, address)

    def obfuscate = copy(emailConfirmKey = None, emailConfirmLastSent = None, passwordReset = None, password = null, userHandle = None)

    def handle: Option[ByteArray] = userHandle.map(handleStr => ByteArray.fromBase64(handleStr))
  }


  implicit val RegisteredUserFormat: Writes[RegisteredUser] = Json.writes[RegisteredUser]
    .transform((obj: JsObject) => obj - "emailConfirmKey" - "passwordReset" - "password")

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
   * @param appId        an internal id for the app
   * @param appCreatedBy the id of the user who created the app
   * @param clientSecret the client secret to authenticate secret (backend to backend) requests
   * @param appName      the name of the app
   */
  case class ApiKey(appId: Option[Int], appCreatedBy: Int, clientSecret: String, appName: String)

  case class ApiKeyData(apiKey: ApiKey, allowedScopes: Set[String], author: UserProfile) {
    def obfuscated = copy(apiKey = apiKey.copy(clientSecret = "********"))
  }

  implicit val ApiKeyFormat: Format[ApiKey] = Json.format[ApiKey]
  implicit val ApiKeyDataFormat: Format[ApiKeyData] = Json.format[ApiKeyData]

  case class CasService(serviceId: Option[Int], serviceName: String, serviceRedirectUrl: Option[String] = None,
                        serviceRequiresFullInfo: Boolean)

  case class SAMLService(serviceId: Option[Int],
                         serviceName: String,
                         issuer: String,
                         metadataEndpoint: Option[String] = None,
                         assertionConsumerService: Option[String] = None,
                         assertionConsumerServiceRegex: Option[String] = None,
                         singleLogoutService: Option[String] = None,
                         singleLogoutServiceRegex: Option[String] = None,
                         serviceRequiresFullInfo: Boolean = false)

  implicit val CasServiceFormat: Format[CasService] = Json.format[CasService]
  implicit val CasServiceRowParser: RowParser[CasService] = Macro.namedParser[CasService](ColumnNaming.SnakeCase)
  implicit val CasServiceParameterList: ToParameterList[CasService] = Macro.toParameters[CasService]()

  implicit val SAMLServiceFormat: Format[SAMLService] = Json.format[SAMLService]
  implicit val SAMLServiceRowParser: RowParser[SAMLService] = Macro.namedParser[SAMLService](ColumnNaming.SnakeCase)
  implicit val SAMLServiceParameterList: ToParameterList[SAMLService] = Macro.toParameters[SAMLService]()

  case class CasV2Ticket(email: String, firstname: String, lastname: String, groups: Set[String])

  implicit val AppRowParser: RowParser[ApiKey] = Macro.namedParser[ApiKey](ColumnNaming.SnakeCase)
  implicit val AppParameterList: ToParameterList[ApiKey] = Macro.toParameters[ApiKey]()

  case class ServiceData(service: CasService, requiredGroups: Set[String], allowedGroups: Set[String], domains: Set[String], accessFrom: List[(Int, String)])

  implicit val ServiceDataFormat: Format[ServiceData] = Json.format[ServiceData]

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
