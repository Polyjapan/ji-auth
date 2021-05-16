package controllers.saml2

import ch.japanimpact.auth.api.cas.StringHelper
import controllers.cas.saml.SAMLv1Parser.{IllegalVersionException, InvalidRequestException}
import controllers.saml2.SAMLv2Parser.SAMLv2AuthnRequest
import controllers.saml2.responses.MetadataResponse
import data.{CASInstance, SAMLv2Instance}
import data.UserSession.RequestWrapper
import models.{ServicesModel, TicketsModel}
import play.api.Logger
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
class SAMLv2Controller @Inject()(cc: ControllerComponents, apps: ServicesModel)
                                (implicit ec: ExecutionContext, xmls: XMLSignService) extends AbstractController(cc) {

  def logoutGet: Action[AnyContent] = TODO

  def metadataGet: Action[AnyContent] = Action { implicit rq =>
    Ok(MetadataResponse()).as("application/xml")
  }

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
    val targetUrl = {
      "https://" + rq.host + rq.path.split('?').head
    }
    try {
      SAMLv2Parser(xml) match {
        case xmlReq: SAMLv2AuthnRequest if xmlReq.destination.isEmpty || xmlReq.destination.contains(targetUrl) =>

          val service = xmlReq.issuer.get
          val redirectUrl = xmlReq.assertionConsumerServiceURL
            .filter(url => StringHelper.getServiceDomain(url) == StringHelper.getServiceDomain(service)) // only same domain allowed!

          // TODO: SAML-profiles 4.1.4.1 "The SP may include a Subject, in which case we must make sure that the logged in user is that subject"

          apps.getCasService(service).map {
            case Some(casService) =>
              if (xmlReq.passive && !rq.hasUserSession) {
                // Gateway: if param is set, we should not attempt to log the user in
                // Check domain:


                Redirect(redirectUrl.orElse(casService.serviceRedirectUrl.map(_.trim)).get)
              } else {
                val ret = Redirect(controllers.routes.RedirectController.redirectGet())

                val instance = SAMLv2Instance(
                  redirectUrl.orElse(casService.serviceRedirectUrl.map(_.trim)).get,
                  RelayState,
                  xmlReq.protocolBinding.getOrElse(SAMLBindings.HTTPPost),
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
        case xmlReq: SAMLv2AuthnRequest  =>
          Logger("SAML").error(s"[SAML Error::Invalid Destination] Current URL: $targetUrl -- Request Dest: ${xmlReq.destination}")
          Future successful BadRequest(views.html.errorPage("Destination invalide", Html("<p>Cette requête SAML est invalide car sa destination est différente de l'URL à laquelle vous avez été redirigé. Merci de signaler cette erreur à l'administrateur du site d'où vous provenez.</p>")));
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
}

