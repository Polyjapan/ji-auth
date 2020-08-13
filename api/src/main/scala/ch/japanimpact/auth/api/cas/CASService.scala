package ch.japanimpact.auth.api.cas

import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}


/**
  * @author Louis Vialar
  */
@Singleton
class CASService @Inject()(val ws: WSClient, val config: CasConfiguration)(implicit ec: ExecutionContext) {
  private val Map = CacheBuilder.newBuilder()
    .expireAfterWrite(30, TimeUnit.SECONDS)
    .build[PGT_IOU, ProxyTicket]()

  /**
    * Call this method when your pgtCallback method is called. This will reccord the association between the
    * PGT IOU and the PGT
    */
  def pgtCallback(pgtIou: String, pgtId: String): Unit = {
    Map.put(pgtIou, pgtId)
  }

  /**
    * Get the URL to which the client should be redirected to login
    */
  def loginUrl(callback: Option[String]) =
    config.path("/login?service=" + callback.getOrElse(config.service))

  /**
    * Use a service ticket to get user information
    *
    * @param ticket the ticket to use (can only be used once, MUST be a ServiceTicket)
    * @param pgtUrl the PGT Callback URL, if you want to get a PGT in the response
    * @return a CASServiceResponse. If `pgtUrl` was defined, and `pgtCallback` called by your PGT Callback endpoint, you
    *         can access the PGT via [[CASServiceResponse.PGT]]
    */
  def serviceValidate(ticket: ServiceTicket, pgtUrl: Option[String] = None): Future[Either[CASError, CASServiceResponse]] =
    ticketValidate(ticket, pgtUrl, "serviceValidate")

  /**
    * Use a service or proxy ticket to get user information
    *
    * @param ticket the ticket to use (can only be used once, MUST be a ServiceTicket or ProxyTicket)
    * @param pgtUrl the PGT Callback URL, if you want to get a PGT in the response
    * @return a CASServiceResponse. If `pgtUrl` was defined, and `pgtCallback` called by your PGT Callback endpoint, you
    *         can access the PGT via [[CASServiceResponse.PGT]]
    */
  def proxyValidate(ticket: ProxyTicket, pgtUrl: Option[String]): Future[Either[CASError, CASServiceResponse]] =
    ticketValidate(ticket, pgtUrl, "proxyValidate")

  /**
    * Get a ProxyTicket for a given service and user.
    *
    * @param ticket           the PGT for that user
    * @param requestedService the service you want to access (the ProxyToken will only be usable by this service)
    * @return a ProxyTicket, that can be used by `requestedService`
    */
  def proxy(ticket: ProxyGrantingTicket, requestedService: String): Future[Either[CASError, ProxyTicket]] = {
    val query = ws.url(config.path("proxy"))
      .withQueryStringParameters(
        "pgt" -> ticket,
        "targetService" -> requestedService,
        "format" -> "JSON"
      )

    query.get()
      .map(_.json)
      .map(js => Json.fromJson[Either[CASError, ProxyTicket]](js).get)
  }


  private def ticketValidate(ticket: String, pgtUrl: Option[String], url: String) = {
    val query = ws.url(config.path(url))
      .withQueryStringParameters(
        "service" -> config.service,
        "ticket" -> ticket,
        "format" -> "JSON"
      )

    (pgtUrl match {
      case Some(url) => query.addQueryStringParameters("pgtUrl" -> url)
      case None => query
    }).get()
      .map(_.json)
      .map(js => Json.fromJson[Either[CASError, CASServiceResponse]](js).get)
      .map(res => res.map(ticket => {
        // Replace PGT_UOI with PGT :)
        ticket.copy(proxyGrantingTicket = ticket.proxyGrantingTicket.flatMap(pgtUoi => {
          val pgt = Option(Map.getIfPresent(pgtUoi))
          Map.invalidate(pgtUoi) // Remove entry from the cache
          pgt
        }))
      }))
  }
}