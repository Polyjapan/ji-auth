package controllers.saml2

import ch.japanimpact.auth.api.cas.StringHelper
import controllers.cas.saml.SAMLv1Parser.{IllegalVersionException, InvalidRequestException}
import controllers.saml2.SAMLv2Parser.SAMLv2AuthnRequest
import data.{CASInstance, SAMLv2Instance}
import data.UserSession.RequestWrapper
import models.{ServicesModel, TicketsModel}
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.twirl.api.Html
import services.XMLSignService
import utils.XMLUtils

import java.nio.charset.Charset
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * Implemented as per `https://apereo.github.io/cas/6.0.x/protocol/CAS-Protocol-Specification.html#27-proxy-cas-20`
 *
 * Implements versions 2 and 3 of CAS protocol
 *
 * @author Louis Vialar
 */
class SAMLv2Controller @Inject()(cc: ControllerComponents, apps: ServicesModel, tickets: TicketsModel, ws: WSClient, xmls: XMLSignService)
                                (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def logoutGet: Action[AnyContent] = TODO

  def metadataGet: Action[AnyContent] = TODO

  def loginPost: Action[Map[String, Seq[String]]] = Action.async(parse.formUrlEncoded.validate(m => {
    if (m.contains("SAMLRequest")) Right(m)
    else Left {
      BadRequest(views.html.errorPage("Requête invalide", Html("<p>Aucune requête SAML présente.</p>")))
    }
  })) { implicit rq =>
    val req = rq.body("SAMLRequest").head
    val relay = rq.body.get("RelayState").flatMap(_.headOption)

    login(req, relay)
  }

  def loginGet(SAMLRequest: String, RelayState: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    login(SAMLRequest, RelayState)
  }

  private def login(SAMLRequest: String, RelayState: Option[String])(implicit rq: Request[_]) = {
    val xml = XMLUtils.decodeURLParamToXML(SAMLRequest, rq.charset.getOrElse(Charset.defaultCharset().name()))
    try {
      SAMLv2Parser(xml) match {
        case xmlReq: SAMLv2AuthnRequest =>
          val service = xmlReq.issuer.get
          val redirectUrl = xmlReq.assertionConsumerServiceURL
            .filter(url => StringHelper.getServiceDomain(url) == StringHelper.getServiceDomain(service)) // only same domain

          apps.getCasService(service).map {
            case Some(casService) =>
              if (xmlReq.passive && !rq.hasUserSession) {
                // Gateway: if param is set, we should not attempt to log the user in
                // Check domain:


                Redirect(redirectUrl.orElse(casService.serviceRedirectUrl.map(_.trim)).get)
              } else {
                val ret = Redirect(controllers.routes.RedirectController.redirectGet())

                // TODO: handle case where Protocol Binding is not set
                val instance = SAMLv2Instance(
                  redirectUrl.orElse(casService.serviceRedirectUrl.map(_.trim)).get,
                  RelayState,
                  xmlReq.protocolBinding.get,
                  issuer = service,
                  xmlReq.requestId,
                  xmlReq.nameIdPolicy.flatMap(_.format).getOrElse(SAMLNameIdFormats.EmailAddressFormat),
                  casService.serviceId.get, requireFullInfo = casService.serviceRequiresFullInfo)

                if (xmlReq.forceAuthn)
                  // if forceauthn is set, we should drop the existing user session and ask the user to log in again
                  ret.withSession(instance.pair)
                else
                  ret.addingToSession(instance.pair)
              }
            case None =>
              NotFound(views.html.errorPage("Service introuvable", Html("<p>Le service spécifié est introuvable. Merci de signaler cette erreur à l'administrateur du site d'où vous provenez.</p>")));
          }
        case _ =>
          Future successful BadRequest(views.html.errorPage("Requête invalide", Html("<p>Cette requête SAML est invalide. Merci de signaler cette erreur à l'administrateur du site d'où vous provenez.</p>")));
      }
    } catch {
      case e: IllegalVersionException =>
        Future successful BadRequest(views.html.errorPage("Version invalide", Html("<p>Cette version de SAML n'est pas supportée. Merci de signaler cette erreur à l'administrateur du site d'où vous provenez.</p>")))

      case e: InvalidRequestException =>
        e.printStackTrace()
        Future successful BadRequest(views.html.errorPage("Requête invalide", Html("<p>Cette requête SAML est invalide. Merci de signaler cette erreur à l'administrateur du site d'où vous provenez.</p>")));

      case e: NoSuchElementException =>
        Future successful BadRequest(views.html.errorPage("Pas d'origine dans la requête", Html("<p>Cette requête SAML ne contient pas de champ ISSUER, obligatoire sur ce service. Merci de signaler cette erreur à l'administrateur du site d'où vous provenez.</p>")));

    }
  }

  /*

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
     *
     * @param target get parameter named TARGET containing the URL encoded service URL
     * @return
     */
    def samlValidate(TARGET: Option[String]): Action[NodeSeq] = Action.async(parse.xml) { implicit rq =>
      // parse the request T_T
      try {
        if (TARGET.isEmpty) Future.successful(NotFound)
        else {
          val request = SAMLv1Parser(rq.body)
          doServiceValidateWithResponseWriter(request.serviceTicket, TARGET.get, SAMLCASWriter(request.requestId, TARGET.get), None, servicePattern)
        }
      } catch {
        case e: IllegalVersionException =>
          Future.successful(BadRequest(SAMLError(TARGET.get)(SAMLError.ErrorType.VersionMismatch)))
        case e: InvalidRequestException =>
          Future.successful(BadRequest(SAMLError(TARGET.get)(SAMLError.ErrorType.Requester)))
      }

    }

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

      val responses: CAS.CASResponses = if (json) CAS.JSONCasResponses else CAS.XMLCasResponses

      if (!pgtPattern.matches(pgt)) {
        Future.successful(responses.getProxyErrorResponse(CASErrorType.InvalidTicketSpec, pgt))
      } else {
        apps.getCasService(service) flatMap {
          case Some(CasService(Some(serviceId), _, _, _)) =>
            tickets.getProxyGrantingTicket(pgt, serviceId) flatMap {
              case Some((userId, true, sessionId)) =>
                apps.hasRequiredGroups(serviceId, userId).map {
                  case true =>
                    val pt = "PT-" + RandomUtils.randomString(64)
                    tickets.insertCasTicket(pt, userId, serviceId, sessionId)

                    responses.getProxySuccessResponse(pt)
                  case false =>
                    responses.getProxyErrorResponse(CASErrorType.InternalError, pgt)
                }
              case None =>
                Future.successful(responses.getProxyErrorResponse(CASErrorType.InvalidTicket, pgt))
            }

          case _ =>
            Future.successful(
              responses.getProxyErrorResponse(CASErrorType.InvalidService, CAS.getServiceDomain(service).getOrElse(service))
            )
        }
      }
    }

    private def createProxyTicket(pgtUrl: String, service: Int, user: Int, sessionId: SessionID): Future[Either[CASErrorType, Option[String]]] = {
      if (!pgtUrl.startsWith("https://")) Future.successful(Left(CASErrorType.UnauthorizedServiceProxy))
      else apps.getCasService(pgtUrl) flatMap {
        case Some(_) =>
          val pgtIou = "PGTIOU-" + RandomUtils.randomString(64)
          val pgtId = "PGT-" + RandomUtils.randomString(64)
          val symbol = if (pgtUrl.contains("?")) "&" else "?"
          val url = pgtUrl + symbol + "pgtId=" + pgtId + "&pgtIou=" + pgtIou

          ws.url(url).get().filter(_.status == 200).flatMap { r =>
            // Insert PGT
            tickets.insertCasProxyTicket(pgtId, user, service, sessionId).map(_ => Right(Some(pgtIou)))
          }.recover {
            case t: Throwable =>
              println("Exception while calling PGT endpoint " + pgtUrl)
              t.printStackTrace()
              Right(None)
          }
        case None => Future(Left(CASErrorType.UnauthorizedServiceProxy))
      }
    }

    private def doServiceValidate(ticket: String, service: String, format: Option[String], pgtUrl: Option[String], pattern: Regex): Action[AnyContent] = Action.async { implicit rq =>
      val json = format.getOrElse("XML").equalsIgnoreCase("json")
      val responses: CAS.CASResponses = if (json) CAS.JSONCasResponses else CAS.XMLCasResponses

      doServiceValidateWithResponseWriter(ticket, service, responses, pgtUrl, pattern)
    }

    private def doServiceValidateWithResponseWriter(ticket: String, service: String, responses: CAS.CASResponses, pgtUrl: Option[String], pattern: Regex): Future[Result] = {
      if (!pattern.matches(ticket)) {
        Future.successful(responses.getErrorResponse(CASErrorType.InvalidTicketSpec, ticket))
      } else {
        apps.getCasService(service) flatMap {
          case Some(CasService(Some(serviceId), _, _, _)) =>
            tickets.getCasTicket(ticket, serviceId) flatMap {
              case Some(((user, sessionKey), groups)) =>
                val ticket = pgtUrl.map(url => createProxyTicket(url, serviceId, user.id.get, sessionKey))
                  .getOrElse(Future.successful(Right(None)))

                val attributes = Map(
                  "email" -> user.email,
                  "name" -> (user.firstName + " " + user.lastName),
                  "firstname" -> user.firstName,
                  "lastname" -> user.lastName,
                )

                ticket.map {
                  case Left(err) =>
                    responses.getErrorResponse(err, pgtUrl.get)
                  case Right(pgt) =>
                    val properties = (pgt match {
                      case Some(t) => Map("proxyGrantingTicket" -> t)
                      case None => Map.empty[String, String]
                    }) + (CASv2Controller.UserProperty -> s"${user.id.get}")

                    responses.getSuccessResponse(properties, attributes, groups)
                }

              case None =>
                Future.successful(responses.getErrorResponse(CASErrorType.InvalidTicket, ticket))
            }

          case None =>
            tickets.invalidateCasTicket(ticket).map(_ =>
              responses.getErrorResponse(CASErrorType.InvalidService, CAS.getServiceDomain(service).getOrElse(service))
            )
        }
      }
    }*/
}

