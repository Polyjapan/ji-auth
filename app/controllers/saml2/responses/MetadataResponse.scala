package controllers.saml2.responses

import play.api.mvc.RequestHeader
import services.XMLSignService

import java.time.Instant

object MetadataResponse extends AbstractSAMLResponse {
  def apply()(implicit signService: XMLSignService, h: RequestHeader): String = {
    val base =
      <md:EntityDescriptor entityID={issuer} validUntil={(Instant.now().plusSeconds(3600)).toString}
                           xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata"
                           xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
                           xmlns:ds="http://www.w3.org/2000/09/xmldsig#">

        <md:Organization>
          <md:OrganizationName xml:lang="en">PolyJapan</md:OrganizationName>
          <md:OrganizationDisplayName xml:lang="en">PolyJapan / Japan Impact</md:OrganizationDisplayName>
          <md:OrganizationURL xml:lang="en">https://www.japan-impact.ch/</md:OrganizationURL>
        </md:Organization>
        <md:ContactPerson contactType="technical">
          <md:SurName>SAML Technical Support</md:SurName>
          <md:EmailAddress>mailto:informatique@japan-impact.ch</md:EmailAddress>
        </md:ContactPerson>

        <md:IDPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol" WantAuthnRequestsSigned="false">
          <md:KeyDescriptor use="signing">
            <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
              <ds:X509Data>
                <ds:X509Certificate>{signService.certContent}</ds:X509Certificate>
              </ds:X509Data>
            </ds:KeyInfo>
          </md:KeyDescriptor>


          <md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</md:NameIDFormat>
          <md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</md:NameIDFormat>
          <md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified</md:NameIDFormat>

          <md:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location={loginUrl}/>
          <md:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location={controllers.saml2.routes.SAMLv2Controller.loginPost().absoluteURL(secure = true)}/>
        </md:IDPSSODescriptor>

      </md:EntityDescriptor>

    signService.signScalaElement(base)
  }

  private def loginUrl(implicit h: RequestHeader) = {
    val url = controllers.saml2.routes.SAMLv2Controller.loginGet("").absoluteURL(secure = true)

    url.take(url.indexOf('?'))
  }
}
