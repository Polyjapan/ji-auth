package ch.japanimpact.auth.api

import ch.japanimpact.auth.api.AuthApi.AppTicketRequest
import ch.japanimpact.auth.api.constants.GeneralErrorCodes.{ErrorCode, RequestError}
import play.api.libs.json.{Format, JsValue, Json}
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}


/**
  * @author Louis Vialar
  */
class AuthApi(val ws: WSClient, val apiBase: String, val apiClientId: String, val apiClientSecret: String) {


  def getAppTicket(ticket: String)(implicit ec: ExecutionContext): Future[Either[AppTicketRequest, ErrorCode]] = {
    ws.url(apiBase + "/api/get_token")
      .post(Json.toJson(AppTicketRequest(ticket, apiClientId, apiClientSecret)))
      .map(r => {
        try {
          if (r.status == 400)
            Right(Json.fromJson[RequestError](r.body[JsValue]).get.errorCode)
          else Left(Json.fromJson[AppTicketRequest](r.body[JsValue]).get)
        } catch {
          case _ => Right(100)
        }
      })
  }
}

object AuthApi {


  implicit val requestMapper: Format[AppTicketRequest] = Json.format[AppTicketRequest]
  implicit val responseMapper: Format[AppTicketResponse] = Json.format[AppTicketResponse]



  class ApiRequest(val endpoint: String, val method: String)

  /**
    * The format of the request sent by the client
    *
    * @param ticket       the ticket the CAS previously sent to the user
    * @param clientId     the clientId of the requesting app
    * @param clientSecret the clientSecret of the requesting app
    */
  case class AppTicketRequest(ticket: String, clientId: String, clientSecret: String) extends ApiRequest("api/get_ticket", "POST")


  /**
    * The object returned when the ticket is found
    *
    * @param userId     the CAS id of user the ticket was generated for
    * @param userEmail  the email of the user the ticket was generated for
    * @param ticketType the type of ticket
    */
  case class AppTicketResponse(userId: Int, userEmail: String, ticketType: TicketType)

}
