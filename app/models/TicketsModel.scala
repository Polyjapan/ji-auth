package models

import java.security.SecureRandom

import anorm.SqlParser._
import anorm._
import data.{RegisteredUser, RegisteredUserRowParser}
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author zyuiop
 */
class TicketsModel @Inject()(dbApi: play.api.db.DBApi, sessions: SessionsModel)(implicit ec: ExecutionContext) {
  private val db = dbApi database "default"


  val rand = new SecureRandom()

  def getCasTicket(token: String, service: Int): Future[Option[(RegisteredUser, Set[String])]] = Future(db.withTransaction { implicit c =>
    val user = SQL"SELECT u.* FROM cas_tickets JOIN users u on cas_tickets.user_id = u.id WHERE ticket = $token AND service_id = $service AND expiration > CURRENT_TIMESTAMP"
      .as(RegisteredUserRowParser.singleOpt)

    val groups = user.map(u => u.id.get).map(uid =>
      SQL"SELECT g.name FROM groups_members JOIN `groups` g on groups_members.group_id = g.id WHERE user_id = $uid"
        .as(str("name").*).toSet
    )

    // CAS: ticket should be invalidated anyway
    SQL"DELETE FROM cas_tickets WHERE ticket = $token".execute()

    user.zip(groups)
  })

  def getProxyTicket(token: String, targetService: Int): Future[Option[(Int, Boolean)]] = Future(db.withTransaction { implicit c =>
    val ret = SQL"SELECT user_id, service_id FROM cas_proxy_tickets WHERE expiration > CURRENT_TIMESTAMP AND ticket = $token"
      .as((int("user_id") ~ int("service_id"))
        .map { case uid ~ sid => (uid, sid) }
        .singleOpt)

    ret.map {
      case (userId, serviceId) =>
        val canIssueToken = SQL"SELECT COUNT(*) FROM cas_proxy_allow WHERE target_service = $targetService AND allowed_service = $serviceId"
          .as(scalar[Int].single) > 0

        (userId, canIssueToken)
    }
  })

  def invalidateCasTicket(token: String) = Future(db.withTransaction { implicit c =>
    SQL"DELETE FROM cas_tickets WHERE ticket = $token".execute()
  })

  def insertCasTicket(token: String, user: Int, service: Int) =
    Future(db.withConnection(implicit c => {
      SQL"INSERT INTO cas_tickets(service_id, user_id, ticket) VALUES ($service, $user, $token)"
        .execute()
    }))

  def insertCasProxyTicket(token: String, user: Int, service: Int) =
    Future(db.withConnection(implicit c => {
      SQL"INSERT INTO cas_proxy_tickets(user_id, service_id, ticket) VALUES ($user, $service, $token)"
        .execute()
    }))

  def logout(user: Int) =
    Future(db.withConnection(implicit c => {
      SQL"DELETE FROM cas_proxy_tickets WHERE user_id = $user"
        .execute()
      SQL"DELETE FROM cas_tickets WHERE user_id = $user"
        .execute()
    }))
}
