package controllers.saml2.responses

import play.api.mvc.RequestHeader
import services.XMLSignService

abstract protected[responses] class AbstractSAMLResponse {

  /**
   * Build the SAMLResponse, possibly signed if needed
   *
   * @param signService the service used to sign the response
   * @param h           a request header, used to determine the absolute URLs.
   * @return a string XML
   */
  def apply()(implicit signService: XMLSignService, h: RequestHeader): String

  protected def issuer(implicit h: RequestHeader): String = controllers.saml2.routes.SAMLv2Controller.metadataGet().absoluteURL(true)

}
