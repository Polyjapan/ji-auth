package data

import play.api.mvc.{RequestHeader, Session}

/**
  * @author Louis Vialar
  */
case class UserSession(id: Int, email: String, sessionKey: SessionID) {
}

object UserSession {

  def apply(ru: RegisteredUser, sid: SessionID): List[(String, String)] =
    List("id" -> ru.id.get.toString, "email" -> ru.email, "sessionKey" -> sid.toString)

  def apply(pair: ((Int, String), SessionID)): UserSession = UserSession(pair._1._1, pair._1._2, pair._2)

  def apply(session: Session): Option[UserSession] = {
    val id = session.get("id").filter(_.nonEmpty).filter(_.forall(_.isDigit)).map(_.toInt)
    val email = session.get("email").filter(_.nonEmpty)
    val sessionKey = session.get("sessionKey").filter(_.nonEmpty).map(str => SessionID(str))

    (id zip email zip sessionKey).map(p => UserSession(p))
  }

  implicit class RequestWrapper(rq: RequestHeader) {
    def hasUserSession: Boolean = UserSession(rq.session).nonEmpty

    def userSession: UserSession = UserSession(rq.session).get
  }
}
