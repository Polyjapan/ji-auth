package models

import java.security.SecureRandom

import anorm.SqlParser._
import anorm._
import data.{RegisteredUser, RegisteredUserRowParser}
import javax.inject.Inject
import utils.RandomUtils

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author zyuiop
 */
class TicketsModel @Inject()(dbApi: play.api.db.DBApi, sessions: SessionsModel)(implicit ec: ExecutionContext) {
  private val db = dbApi database "default"


  val rand = new SecureRandom()

  def getCasTicket(token: String, service: Int): Future[Option[(RegisteredUser, Set[String])]] = Future(db.withTransaction { implicit c =>
    val user = SQL"SELECT u.* FROM cas_tickets JOIN users u on cas_tickets.user_id = u.id WHERE ticket = $token AND expiration > CURRENT_TIMESTAMP"
      .as(RegisteredUserRowParser.singleOpt)

    val groups = user.map(u => u.id.get).map(uid =>
      SQL"SELECT g.name FROM groups_members JOIN `groups` g on groups_members.group_id = g.id WHERE user_id = $uid"
        .as(str("name").*).toSet
    )

    user.zip(groups)
  })

  /**
   * Create a ticket for a user
   *
   * @return a future holding the generated token for this ticket
   */
  def createCasTicketForUser(user: Int, service: Int): Future[String] = {
    val token = RandomUtils.randomString(64)

    Future(db.withConnection(implicit c => {
      SQL"INSERT INTO cas_tickets(service_id, user_id, ticket) VALUES ($service, $user, $token)"
        .execute()
      token
    }))
  }
}
