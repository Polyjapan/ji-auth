package data

import play.api.mvc.{RequestHeader, Session}

/**
  * @author Louis Vialar
  */
case class UserSession(id: Int, email: String, adminLevel: Int) {
  def hasPermission(perm: Int): Boolean = (adminLevel & perm) != 0
  lazy val canCreateApiKey: Boolean = hasPermission(UserSession.ManageApiKeys)
  lazy val canCreateCasApp: Boolean = hasPermission(UserSession.ManageCasApps)
  lazy val canCreateInternalApp: Boolean = hasPermission(UserSession.ManageInternalApps)
  lazy val canManageGroups: Boolean = hasPermission(UserSession.ManageGroups)
  lazy val canChangePerms: Boolean = hasPermission(UserSession.ChangeOtherPermissions)
  lazy val canBrowseUsers: Boolean = hasPermission(UserSession.BrowseUsers)
  lazy val isSuperAdmin: Boolean = hasPermission(UserSession.SuperAdmin)

  lazy val isAdmin: Boolean = {
     canCreateApiKey || canCreateCasApp || canCreateInternalApp || canManageGroups || canChangePerms || canBrowseUsers || isSuperAdmin
  }
}

object UserSession {
  val SuperAdmin: Int = 64 // Cannot be edited with a simple flag ChangeOtherPermissions
  val ChangeOtherPermissions: Int = 32
  val ManageInternalApps: Int = 16
  val ManageApiKeys: Int = 8
  val ManageCasApps: Int = 4
  val ManageGroups: Int = 2
  val BrowseUsers: Int = 1

  def apply(ru: RegisteredUser): List[(String, String)] =
    List("id" -> ru.id.get.toString, "email" -> ru.email, "admin_level" -> ru.adminLevel.toString)

  def apply(pair: ((Int, String), Int)): UserSession = UserSession(pair._1._1, pair._1._2, pair._2)

  def apply(session: Session): Option[UserSession] = {
    val id = session.get("id").filter(_.nonEmpty).filter(_.forall(_.isDigit)).map(_.toInt)
    val email = session.get("email").filter(_.nonEmpty)
    val adminLevel = session.get("admin_level").filter(_.nonEmpty).filter(_.forall(_.isDigit)).map(_.toInt)

    (id zip email zip adminLevel).map(p => UserSession(p)).headOption
  }

  implicit class RequestWrapper(rq: RequestHeader) {
    def hasUserSession: Boolean = UserSession(rq.session).nonEmpty

    def userSession: UserSession = UserSession(rq.session).get
  }
}
