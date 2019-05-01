package data

import play.api.mvc.{RequestHeader, Session}

/**
  * @author Louis Vialar
  */
case class UserSession(id: Int, email: String)

object UserSession {
  def apply(ru: RegisteredUser): List[(String, String)] =
    List("id" -> ru.id.get.toString, "email" -> ru.email)

  def apply(pair: (Int, String)): UserSession = UserSession(pair._1, pair._2)

  def apply(session: Session): Option[UserSession] = {
    val id = session.get("id").filter(_.nonEmpty).filter(_.forall(_.isDigit)).map(_.toInt)
    val email = session.get("email").filter(_.nonEmpty)

    (id zip email).map(p => UserSession(p)).headOption
  }

  implicit class RequestWrapper(rq: RequestHeader) {
    def hasUserSession: Boolean = UserSession(rq.session).nonEmpty

    def userSession: UserSession = UserSession(rq.session).get
  }
}
