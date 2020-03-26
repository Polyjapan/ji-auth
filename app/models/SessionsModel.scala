package models

import anorm.SqlParser._
import anorm._
import data.SessionID
import javax.inject.Inject
import utils.RandomUtils

import scala.concurrent.{ExecutionContext, Future}

class SessionsModel @Inject()(dbApi: play.api.db.DBApi)(implicit ec: ExecutionContext) {
  private val db = dbApi database "default"

  def createSession(userId: Int): Future[SessionID] = Future(db.withConnection { implicit conn =>
    val sessId = RandomUtils.randomSession
    SQL"INSERT INTO sessions(session_key, user_id, expires_at) VALUES (${sessId.bytes}, $userId, TIMESTAMPADD(DAY, 31, CURRENT_TIMESTAMP))"
      .execute()

    sessId
  })

  def getSession(sessId: SessionID): Future[Option[(Int, Set[String])]] = Future(db.withConnection { implicit conn =>
    val lst = SQL"SELECT sessions.user_id, g.name FROM sessions LEFT JOIN groups_members gm on sessions.user_id = gm.user_id LEFT JOIN `groups` g on gm.group_id = g.id WHERE session_key = ${sessId.bytes} AND expires_at > CURRENT_TIMESTAMP"
      .as((int("user_id") ~ str("name")).map { case a ~ b => (a, b) }.*)

    lst.headOption.map {
      case (id, _) => (id, lst.map(_._2).toSet)
    }
  })
}
