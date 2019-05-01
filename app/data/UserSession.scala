package data

import play.api.mvc.{RequestHeader, Session}

/**
  * @author Louis Vialar
  */
case class UserSession(id: Int, email: String, adminLevel: Int) {
  def canCreateApp: Boolean = adminLevel >= UserSession.CreateAppLevel
  def canCreateGroup: Boolean = adminLevel >= UserSession.CreateGroupLevel
  def canManageGroup: Boolean = adminLevel >= UserSession.ManageGroups
  def canChangePerms: Boolean = adminLevel >= UserSession.ChangeOtherPermissions
}

object UserSession {
  val ChangeOtherPermissions: Int = 20
  val ManageGroups: Int = 15 // Delete others' groups
  val CreateAppLevel: Int = 10
  val CreateGroupLevel: Int = 5

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
