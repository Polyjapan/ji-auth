import java.sql.Timestamp

import data._
import slick.jdbc.MySQLProfile.api._

/**
  * @author Louis Vialar
  */
package object models {
  private[models] class Clients(tag: Tag) extends Table[Client](tag, "clients") {
    def id = column[Int]("client_id", O.PrimaryKey, O.AutoInc)
    def firstname = column[String]("client_firstname", O.SqlType("VARCHAR(100)"))
    def lastname = column[String]("client_lastname", O.SqlType("VARCHAR(100)"))
    def email = column[String]("client_email", O.SqlType("VARCHAR(180)"), O.Unique)
    def emailConfirmKey = column[Option[String]]("client_email_confirm_key", O.SqlType("VARCHAR(100)"))
    def password = column[String]("client_password", O.SqlType("VARCHAR(250)"))
    def passwordAlgo = column[String]("client_password_algo", O.SqlType("VARCHAR(15)"))
    def passwordReset = column[Option[String]]("client_password_reset", O.SqlType("VARCHAR(250)"))
    def passwordResetEnd = column[Option[Timestamp]]("client_password_reset_end")
    def acceptNews = column[Boolean]("client_accept_newsletter", O.Default(false))

    def * =
      (id.?, firstname, lastname, email, emailConfirmKey, password, passwordAlgo, passwordReset, passwordResetEnd, acceptNews).shaped <> (Client.tupled, Client.unapply)
  }

  private[models] val clients = TableQuery[Clients]

  private[models] class Permissions(tag: Tag) extends Table[(Int, String)](tag, "permissions") {
    def userId = column[Int]("client_id")
    def permission = column[String]("permission", O.SqlType("VARCHAR(180)"))

    def user = foreignKey("permissions_client_fk", userId, clients)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (userId, permission)

    def pk = primaryKey("pk_permissions", (userId, permission))
  }

  private[models] val permissions = TableQuery[Permissions]

  private[models] class RefreshTokens(tag: Tag) extends Table[RefreshToken](tag, "refresh_tokens") {


    def clientID = column[Int]("client_id")

    def token = column[String]("token", O.SqlType("VARCHAR(100)"), O.PrimaryKey)
    def userAgent = column[String]("user_agent", O.SqlType("VARCHAR(250)"))
    def lastIp = column[String]("last_ip", O.SqlType("VARCHAR(32)"))

    def validFrom = column[Timestamp]("valid_from")
    def validTo = column[Timestamp]("valid_to")
    def lastUse= column[Timestamp]("last_use")

    def user = foreignKey("refresh_token_client_fk", clientID, clients)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * =
      (token, clientID, validFrom, validTo, userAgent, lastUse, lastIp).shaped <> (RefreshToken.tupled, RefreshToken.unapply)
  }

  private[models] val refreshTokens = TableQuery[RefreshTokens]

  private[models] class Apps(tag: Tag) extends Table[App](tag, "apps") {
    def id = column[Int]("app_id", O.PrimaryKey, O.AutoInc)

    def clientId = column[String]("client_id", O.SqlType("VARCHAR(150)"))
    def clientSecret = column[String]("client_secret", O.SqlType("VARCHAR(150)"))
    def appName  = column[String]("app_name", O.SqlType("VARCHAR(150)"))
    def redirectUrl = column[String]("redirect_url", O.SqlType("VARCHAR(250)"))

    def * = (id.?, clientId, clientSecret, appName, redirectUrl).shaped <> (App.tupled, App.unapply)
  }

  private[models] val apps = TableQuery[Apps]
}
