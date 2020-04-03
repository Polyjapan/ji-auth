package controllers.cas

import data.CasService
import javax.inject.Inject
import models.{ServicesModel, TicketsModel}
import play.api.libs.ws.WSClient
import play.api.mvc._
import utils.{CAS, RandomUtils}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

/**
 * Implemented as per `https://apereo.github.io/cas/6.0.x/protocol/CAS-Protocol-Specification.html#27-proxy-cas-20`
 *
 * @author Louis Vialar
 */
class CASv2Controller @Inject()(cc: ControllerComponents, apps: ServicesModel, tickets: TicketsModel, ws: WSClient)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val servicePattern: Regex = "^ST-[a-zA-Z0-9_-]{32,128}$".r
  private val proxyPattern = "^[SP]T-[a-zA-Z0-9_-]{32,128}$".r
  private val pgtPattern = "^PGT-[a-zA-Z0-9_-]{32,128}$".r

  /**
   * Validate a ticket and return the associated data. <br>
   * proxyValidate accepts both service tickets (ST-) and proxy tickets (PT-)
   *
   * @param ticket  a service or proxy ticket
   * @param service the service to access
   * @param format  the format of the reply (default XML, avail. JSON)
   * @param pgtUrl  the URL of the proxy to grant a PGT to
   * @return
   */
  def proxyValidate(ticket: String, service: String, format: Option[String], pgtUrl: Option[String]): Action[AnyContent] =
    doServiceValidate(ticket, service, format, pgtUrl, proxyPattern)

  /**
   * Validate a ticket and return the associated data. <br>
   * serviceValidate accepts only service tickets (ST-)
   *
   * @param ticket  a service or proxy ticket
   * @param service the service to access
   * @param format  the format of the reply (default XML, avail. JSON)
   * @param pgtUrl  the URL of the proxy to grant a PGT to
   * @return
   */
  def serviceValidate(ticket: String, service: String, format: Option[String], pgtUrl: Option[String]): Action[AnyContent] =
    doServiceValidate(ticket, service, format, pgtUrl, servicePattern)

  /**
   * Generate a proxyTicket from a PGT
   *
   * @param pgt     the pgt
   * @param service the service to which the PT should be bound
   * @param format
   * @return
   */
  def proxy(pgt: String, service: String, format: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    // json support is not standard but may be practical
    val json = format.getOrElse("XML").equalsIgnoreCase("json")

    if (!pgtPattern.matches(pgt)) {
      Future.successful(if (json)
        Ok(CAS.getCasErrorResponseJson(CAS.CASError.InvalidTicketSpec, pgt))
      else
        Ok(CAS.getProxyErrorResponseXML(CAS.CASError.InvalidTicketSpec, pgt)))
    } else {
      apps.getCasService(service) flatMap {
        case Some(CasService(serviceId, _, _)) =>
          tickets.getProxyTicket(pgt, serviceId) flatMap {
            case Some((userId, true)) =>
              apps.hasRequiredGroups(serviceId, userId).map {
                case true =>
                  val pt = "PT-" + RandomUtils.randomString(64)
                  tickets.insertCasTicket(pt, userId, serviceId)

                  if (json)
                    Ok(CAS.getProxySuccessJson(pt))
                  else
                    Ok(CAS.getProxySuccessXML(pt))
                case false =>
                  if (json) Ok(CAS.getCasErrorResponseJson(CAS.CASError.InternalError, pgt))
                  else Ok(CAS.getCasErrorResponseXML(CAS.CASError.InternalError, pgt))
              }
            case None =>
              Future.successful(
                if (json) Ok(CAS.getCasErrorResponseJson(CAS.CASError.InvalidTicket, pgt))
                else Ok(CAS.getCasErrorResponseXML(CAS.CASError.InvalidTicket, pgt))
              )
          }

        case _ =>
          Future.successful(
            if (json)
              Ok(CAS.getCasErrorResponseJson(CAS.CASError.InvalidService, CAS.getServiceDomain(service).getOrElse(service)))
            else
              Ok(CAS.getProxyErrorResponseXML(CAS.CASError.InvalidService, CAS.getServiceDomain(service).getOrElse(service)))
          )
      }
    }
  }

  private def createProxyTicket(pgtUrl: String, service: Int, user: Int): Future[Either[CAS.CASError.CASErrorType, Option[String]]] = {
    if (!pgtUrl.startsWith("https://")) Future.successful(Left(CAS.CASError.UnauthorizedServiceProxy))
    else apps.getCasService(pgtUrl) flatMap {
      case Some(_) =>
        val pgtIou = "PGTIOU-" + RandomUtils.randomString(64)
        val pgtId = "PGT-" + RandomUtils.randomString(64)
        val symbol = if (pgtUrl.contains("?")) "&" else "?"
        val url = pgtUrl + symbol + "pgtId=" + pgtId + "&pgtIou=" + pgtIou

        ws.url(url).get().filter(_.status == 200).flatMap { r =>
          // Insert PGT
          tickets.insertCasProxyTicket(pgtId, user, service).map(_ => Right(Some(pgtIou)))
        }.fallbackTo(Future.successful(Right(None)))
      case None => Future(Left(CAS.CASError.UnauthorizedServiceProxy))
    }
  }

  private def doServiceValidate(ticket: String, service: String, format: Option[String], pgtUrl: Option[String], pattern: Regex): Action[AnyContent] = Action.async { implicit rq =>
    val json = format.getOrElse("XML").equalsIgnoreCase("json")

    if (!pattern.matches(ticket)) {
      Future.successful(if (json)
        Ok(CAS.getCasErrorResponseJson(CAS.CASError.InvalidTicketSpec, ticket))
      else
        Ok(CAS.getCasErrorResponseXML(CAS.CASError.InvalidTicketSpec, ticket)))
    } else {
      apps.getCasService(service) flatMap {
        case Some(CasService(serviceId, _, _)) =>
          tickets.getCasTicket(ticket, serviceId) flatMap {
            case Some((user, groups)) =>
              val ticket = pgtUrl.map(url => createProxyTicket(url, serviceId, user.id.get))
                .getOrElse(Future.successful(Right(None)))

              val attributes = Map(
                "email" -> user.email,
                "name" -> (user.firstName + " " + user.lastName),
                "firstname" -> user.firstName,
                "lastname" -> user.lastName
              )

              ticket.map {
                case Left(err) =>
                  if (json) Ok(CAS.getCasErrorResponseJson(err, pgtUrl.get))
                  else Ok(CAS.getCasErrorResponseXML(err, pgtUrl.get))
                case Right(pgt) =>
                  val properties = (pgt match {
                    case Some(t) => Map("proxyGrantingTicket" -> t)
                    case None => Map.empty[String, String]
                  }) + ("user" -> s"${user.id.get}")

                  if (json) Ok(CAS.getCasSuccessMessageJson(properties, attributes, groups))
                  else Ok(CAS.getCasSuccessMessageXML(properties, attributes, groups))
              }

            case None =>
              Future.successful(
                if (json) Ok(CAS.getCasErrorResponseJson(CAS.CASError.InvalidTicket, ticket))
                else Ok(CAS.getCasErrorResponseXML(CAS.CASError.InvalidTicket, ticket)))
          }

        case None =>
          tickets.invalidateCasTicket(ticket).map(_ =>
            if (json) Ok(CAS.getCasErrorResponseJson(CAS.CASError.InvalidService, CAS.getServiceDomain(service).getOrElse(service)))
            else Ok(CAS.getCasErrorResponseXML(CAS.CASError.InvalidService, CAS.getServiceDomain(service).getOrElse(service)))
          )
      }
    }
  }
}
