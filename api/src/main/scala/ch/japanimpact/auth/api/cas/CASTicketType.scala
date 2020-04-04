package ch.japanimpact.auth.api.cas

import scala.collection.mutable

sealed abstract class CASTicketType(prefix: String) {
  CASTicketType.register(prefix, this)

  def isInstance(ticket: String) = ticket.startsWith(prefix + "-")
}


object CASTicketType {
  private val Map = mutable.Map[String, CASTicketType]();

  private def register(prefix: String, tpe: CASTicketType): Unit = {
    Map.put(prefix, tpe)
  }

  def apply(token: String): Option[CASTicketType] = Map.get(token.split("-")(0))

  case object ServiceTicket extends CASTicketType("ST")
  case object ProxyTicket extends CASTicketType("PT")
  case object ProxyGrantingTicket extends CASTicketType("PGT")
  case object ProxyGrantingTicketIOU extends CASTicketType("PGTIOU")
}