import java.sql.Timestamp

import ch.japanimpact.auth.api.TicketType
import data._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._

/**
  * The different models mapping classes from [[data]] to actual SQL tables
  *
  * @author Louis Vialar
  */
package object models {

  private[models] class RegisteredUsers(tag: Tag) extends Table[RegisteredUser](tag, "users") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def email = column[String]("email", O.SqlType("VARCHAR(180)"), O.Unique)

    def emailConfirmKey = column[Option[String]]("email_confirm_key", O.SqlType("VARCHAR(100)"))

    def password = column[String]("password", O.SqlType("VARCHAR(250)"))

    def passwordAlgo = column[String]("password_algo", O.SqlType("VARCHAR(15)"))

    def passwordReset = column[Option[String]]("password_reset", O.SqlType("VARCHAR(250)"))

    def passwordResetEnd = column[Option[Timestamp]]("password_reset_end")

    def adminLevel = column[Int]("admin_level")

    def * =
      (id.?, email, emailConfirmKey, password, passwordAlgo, passwordReset, passwordResetEnd, adminLevel).shaped <> (RegisteredUser.tupled, RegisteredUser.unapply)
  }

  private[models] val registeredUsers = TableQuery[RegisteredUsers]

  private[models] class Apps(tag: Tag) extends Table[App](tag, "apps") {
    def id = column[Int]("app_id", O.PrimaryKey, O.AutoInc)

    def userId = column[Int]("app_created_by")

    def clientId = column[String]("client_id", O.SqlType("VARCHAR(150)"))

    def clientSecret = column[String]("client_secret", O.SqlType("VARCHAR(150)"))

    def appName = column[String]("app_name", O.SqlType("VARCHAR(150)"))

    def redirectUrl = column[String]("redirect_url", O.SqlType("VARCHAR(250)"))

    def emailRedirectUrl = column[String]("email_callback_url", O.SqlType("VARCHAR(250)"))

    def captchaPrivate = column[String]("recaptcha_private_key", O.SqlType("VARCHAR(250)"))

    def user = foreignKey("apps_users_fk", userId, registeredUsers)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, userId, clientId, clientSecret, appName, redirectUrl, emailRedirectUrl, captchaPrivate.?).shaped <> (App.tupled, App.unapply)
  }

  private[models] val apps = TableQuery[Apps]

  implicit private val typeMap: JdbcType[TicketType] with BaseTypedType[TicketType] = MappedColumnType.base[TicketType, String](TicketType.unapply, TicketType.apply)

  private[models] class Tickets(tag: Tag) extends Table[Ticket](tag, "tickets") {
    def token = column[String]("token", O.SqlType("VARCHAR(100)"), O.PrimaryKey)

    def userID = column[Int]("user_id")

    def appID = column[Int]("app_id")

    def validTo = column[Timestamp]("valid_to")

    def ticketType = column[TicketType]("type")

    def user = foreignKey("tickets_users_fk", userID, registeredUsers)(_.id, onDelete = ForeignKeyAction.Cascade)

    def app = foreignKey("tickets_apps_fk", appID, apps)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * =
      (token, userID, appID, validTo, ticketType).shaped <> (Ticket.tupled, Ticket.unapply)
  }

  private[models] val tickets = TableQuery[Tickets]

  private[models] class Groups(tag: Tag) extends Table[Group](tag, "groups") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def userId = column[Int]("owner")

    def name = column[String]("name", O.SqlType("VARCHAR(100)"))

    def displayName = column[String]("display_name", O.SqlType("VARCHAR(100)"))

    def user = foreignKey("groups_users_fk", userId, registeredUsers)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * =
      (id.?, userId, name, displayName).shaped <> (Group.tupled, Group.unapply)
  }

  private[models] val groups = TableQuery[Groups]

  private[models] class GroupMembers(tag: Tag) extends Table[GroupMember](tag, "group_members") {
    def groupId = column[Int]("group_id", O.PrimaryKey)

    def userId = column[Int]("owner_id", O.PrimaryKey)

    def canManage = column[Boolean]("can_manage")
    def canRead = column[Boolean]("can_read")
    def isAdmin = column[Boolean]("is_admin")

    def user = foreignKey("groups_members_users_fk", userId, registeredUsers)(_.id, onDelete = ForeignKeyAction.Cascade)
    def group = foreignKey("groups_members_groups_fk", groupId, groups)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * =
      (groupId, userId, canManage, canRead, isAdmin).shaped <> (GroupMember.tupled, GroupMember.unapply)
  }

  private[models] val groupMembers = TableQuery[GroupMembers]
}
