package models

import java.security.SecureRandom
import java.sql.Timestamp

import ch.japanimpact.tools.TicketType
import data.{RegisteredUser, Ticket}
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.dbio.Effect.All
import slick.jdbc.MySQLProfile
import utils.RandomUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author zyuiop
  */
class TicketsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  val rand = new SecureRandom()

  /**
    * Create a [[data.Ticket]] for a user
    *
    * @return a future holding the generated token for this ticket
    */
  def createTicketForUser(user: Int, app: Int, ticketType: TicketType): Future[String] = {
    val token = RandomUtils.randomString(64)
    val endTime = new Timestamp(System.currentTimeMillis() + 24 * 3600 * 1000L) // Valid 24 hours

    val ticket = Ticket(token, user, app, endTime, ticketType)

    db.run(tickets += ticket).map(_ => token)
  }

  /**
    * Checks that a token exists, invalidates it then returns the associated data
    *
    * @param token  the token to check
    * @param client the client that this token belongs to
    * @return
    */
  def useTicket(token: String, client: data.App): Future[Option[(Ticket, RegisteredUser)]] = {
    db.run {
      tickets
        .filter(row => row.appID === client.id && row.token === token) // get tickets corresponding to the request
        .join(registeredUsers).on((ticket, user) => ticket.userID === user.id) // joining with the user data
        .result
        .headOption
        .flatMap[Option[(Ticket, RegisteredUser)], NoStream, All](found => {
        if (found.isDefined) { // If we found data, delete it (= invalidate ticket)
          tickets.filter(row => row.token === token && row.appID === client.id).delete.map(_ => found)
        } else DBIO.successful[Option[(Ticket, RegisteredUser)]](found)
      })
    }
  }
}
