import java.sql.Timestamp

/**
  * @author Louis Vialar
  */
package object data {

  /**
    * Defines a Client
    *
    * @param id               the id of the client in database
    * @param lastname         the last name of the client
    * @param firstname        the first name of the client
    * @param email            the email of the client
    * @param emailConfirmKey  an optional confirmation key for the email. If it's null then the email has been validated
    * @param password         the hashed password of the user
    * @param passwordAlgo     the algorithm used to hash the password
    * @param passwordReset    an optional reset key for the password. If it's null then no password change was requested
    * @param passwordResetEnd an optional timestamp marking the date at which the password reset key will no longer be valid
    *                         If absent, the password reset key is considered invalid
    */
  case class Client(id: Option[Int], lastname: String, firstname: String, email: String, emailConfirmKey: Option[String], password: String,
                    passwordAlgo: String, passwordReset: Option[String] = Option.empty, passwordResetEnd: Option[Timestamp] = Option.empty,
                    acceptNewsletter: Boolean)

  /**
    * Represents a refresh token in the system. When authenticating successfully, the user will be returned to the app with a refresh token.
    * The user can then obtain short-lived access tokens using the returned refresh token.
    * @param token the token
    * @param clientId the id of the client this token is bound to
    * @param validFrom the start of validity for this token
    * @param validTo the last date of validity for this token
    * @param userAgent the user agent this token was last used with
    * @param lastUse the last date this token was used
    * @param lastIp the last ip that used this token
    */
  case class RefreshToken(token: String, clientId: Int, validFrom: Timestamp, validTo: Timestamp, userAgent: String, lastUse: Timestamp, lastIp: String)

  /**
    * Represents an app allowed in the system
    * @param id an internal id for the app
    * @param clientId the clientID to provide in requests
    * @param clientSecret the client secret to authenticate secret (backend to backend) requests
    * @param appName the name of the app
    * @param redirectUrl the URL where the user should be redirected after logging in
    */
  case class App(id: Option[Int], clientId: String, clientSecret: String, appName: String, redirectUrl: String)
}
