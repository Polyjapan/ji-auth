package models

import java.security.SecureRandom
import java.sql.Timestamp

import ch.japanimpact.auth.api.TicketType
import data.{RegisteredUser, RegisteredUserRowParser, Ticket, TicketRowParser, ticketTypes}
import javax.inject.Inject
import utils.RandomUtils
import anorm._
import anorm.SqlParser._

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author zyuiop
 */
class TicketsModel @Inject()(dbApi: play.api.db.DBApi, sessions: SessionsModel)(implicit ec: ExecutionContext) {
  private val db = dbApi database "default"


  val rand = new SecureRandom()

  def getCasTicket(token: String, service: Int): Future[Option[RegisteredUser]] = Future(db.withTransaction { implicit c =>
    SQL"SELECT u.* FROM cas_tickets JOIN users u on cas_tickets.user_id = u.id WHERE ticket = $token AND expiration > CURRENT_TIMESTAMP"
      .as(RegisteredUserRowParser.singleOpt)
  })

  /**
   * Create a [[data.Ticket]] for a user
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

  /**
   * Create a [[data.Ticket]] for a user
   *
   * @return a future holding the generated token for this ticket
   */
  def createTicketForUser(user: Int, app: Int, ticketType: TicketType): Future[String] = {
    val token = RandomUtils.randomString(64)
    val endTime = new Timestamp(System.currentTimeMillis() + 24 * 3600 * 1000L) // Valid 24 hours

    Future(db.withConnection(implicit c => {
      SQL"INSERT INTO tickets(token, user_id, app_id, valid_to, type) VALUES ($token, $user, $app, $endTime, ${ticketTypes(ticketType)})"
          .execute()
      token
    }))
  }

  /**
   * Checks that a token exists, invalidates it then returns the associated data
   *
   * @param token  the token to check
   * @param client the client that this token belongs to
   * @return
   */
  def useTicket(token: String, client: data.App): Future[Option[(Ticket, RegisteredUser)]] = Future(db.withTransaction { implicit c =>
    val res = SQL"SELECT * FROM tickets JOIN users u on tickets.user_id = u.id WHERE token = $token AND app_id = ${client.appId.get}"
        .as((TicketRowParser ~ RegisteredUserRowParser).map{ case t ~ u => (t, u)}.singleOpt)

    SQL"DELETE FROM tickets WHERE token = $token AND app_id = ${client.appId.get}".execute()

    res
  })
}
