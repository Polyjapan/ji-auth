package data

import play.api.libs.json.{Json, Writes}

/**
  * The object returned on a login success
  *
  * @param ticket the ticket the app can use to get the user data
  */
case class LoginSuccess(ticket: String)

object LoginSuccess {
  implicit val successWrites: Writes[LoginSuccess] = Json.writes[LoginSuccess]
}