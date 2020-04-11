package models

import anorm.SqlParser._
import anorm._
import ch.japanimpact.auth.api
import data.SessionID
import javax.inject.Inject
import utils.RandomUtils

import scala.concurrent.{ExecutionContext, Future}

class SessionsModel @Inject()(dbApi: play.api.db.DBApi, usersModel: UsersModel)(implicit ec: ExecutionContext) {
  private val db = dbApi database "default"

  def createSession(userId: Int, ip: String, ua: String): Future[SessionID] = Future(db.withConnection { implicit conn =>
    val sessId = RandomUtils.randomSession
    SQL"INSERT INTO sessions(session_key, user_id, user_agent, ip_address) VALUES (${sessId.bytes}, $userId, $ua, $ip)"
      .execute()

    sessId
  })

  def getSession(sessId: SessionID): Future[Option[api.UserData]] = Future(db.withConnection { implicit conn =>
    SQL"SELECT user_id FROM sessions WHERE session_key = ${sessId.bytes} AND expires_at > CURRENT_TIMESTAMP".as(scalar[Int].singleOpt)
  }).flatMap {
    case Some(id) => usersModel.getUserData(id)
    case None => Future.successful(None)
  }

  def logoutUser(user: Int) =
    Future(db.withConnection(implicit c => {
      SQL"DELETE FROM sessions WHERE user_id = $user"
        .execute()
    }))

  def logoutSession(session: SessionID) =
    Future(db.withConnection(implicit c => {
      SQL"DELETE FROM sessions WHERE session_key = ${session.bytes}"
        .execute()
    }))
}
