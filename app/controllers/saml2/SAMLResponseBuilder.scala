package controllers.saml2

import ch.japanimpact.auth.api.UserData
import ch.japanimpact.auth.api.cas.StringHelper
import data.SAMLv2Instance
import play.api.mvc.RequestHeader
import services.XMLSignService
import utils.RandomUtils

import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/*
    DO NOT REFORMAT THIS FILE!!!!!!

    INTELLIJ CREATES NEWLINES IN THE XML WHICH MAKES THE VALUES INCORRECT.
 */

class SAMLResponseBuilder @Inject()(val signService: XMLSignService) {

  def metadata(implicit h: RequestHeader) = {
    val loginUrl = {
      val url = controllers.saml2.routes.SAMLv2Controller.loginGet("").absoluteURL(true)

      url.take(url.indexOf('?'))
    }

    val base = <md:EntityDescriptor entityID={issuer} validUntil={(Instant.now().plusSeconds(300)).toString}
                                    xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata"
                                    xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
                                    xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
      <!-- insert ds:Signature element (omitted) -->
      <!-- insert md:IDPSSODescriptor element (below) -->
      <md:Organization>
        <md:OrganizationName xml:lang="en">PolyJapan</md:OrganizationName>
        <md:OrganizationDisplayName xml:lang="en">PolyJapan / Japan Impact</md:OrganizationDisplayName>
        <md:OrganizationURL xml:lang="en">https://www.japan-impact.ch/</md:OrganizationURL>
      </md:Organization>
      <md:ContactPerson contactType="technical">
        <md:SurName>SAML Technical Support</md:SurName>
        <md:EmailAddress>mailto:informatique@japan-impact.ch</md:EmailAddress>
      </md:ContactPerson>

      <md:IDPSSODescriptor
      protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
        <!--
        TODO
        <md:KeyDescriptor use="signing">
          <ds:KeyInfo>...</ds:KeyInfo>
        </md:KeyDescriptor> -->

        <md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</md:NameIDFormat>
        <md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</md:NameIDFormat>

        <md:SingleSignOnService
        Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"
        Location={loginUrl}/>
        <md:SingleSignOnService
        Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
        Location={controllers.saml2.routes.SAMLv2Controller.loginPost().absoluteURL(true)}/>
      </md:IDPSSODescriptor>

    </md:EntityDescriptor>

    signService.signScalaElement(base)

  }

  def success(instance: SAMLv2Instance, user: UserData)(implicit r: RequestHeader): String = {
    val nameID = {
      if (instance.nameIDFormat == SAMLNameIdFormats.EmailAddressFormat) user.email
      else user.id.get.toString
    }
    val audience = instance.issuer

    val attributes = {
      val src: Map[String, String] = Map(
        "id" -> user.id.get.toString,
        "email" -> user.email,
        "name" -> (user.details.firstName + " " + user.details.lastName),
        "firstname" -> user.details.firstName,
        "lastname" -> user.details.lastName,
      )

      if (instance.requireFullInfo) {
        src updated("phoneNumber", user.details.phoneNumber.get)
      } else src
    }


    build(nameID, issuer, audience, attributes, user.groups, instance)
  }

  private def issuer(implicit h: RequestHeader): String = controllers.saml2.routes.SAMLv2Controller.metadataGet().absoluteURL(true)

  private def build(nameID: String, issuer: String, audience: String,
                    attributes: Map[String, String], groups: Set[String], instance: SAMLv2Instance): String = {
    val now = Instant.now().toString
    val id = "pfx" + UUID.randomUUID().toString
    val assertId = random()

    val assertion = buildAssertion(assertId, nameID, issuer, now, audience, attributes, groups, instance)


    //@formatter:off
    val data = <samlp:Response
    xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
    xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
    ID={id}
    InResponseTo={instance.requestId}
    Version="2.0"
    IssueInstant={now}
    Destination={instance.url}>
      <saml:Issuer>{issuer}</saml:Issuer>
      <samlp:Status>
        <samlp:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
      </samlp:Status>{assertion}
    </samlp:Response>
    //@formatter:on


    signService.signScalaElement(data)
  }

  private def random() = "SAML-" + RandomUtils.randomString(16)

  private def buildAssertion(index: String, nameID: String, issuer: String, now: String, audience: String,
                             attributes: Map[String, String], groups: Set[String], instance: SAMLv2Instance) = {


    //@formatter:off

    <saml:Assertion xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xmlns:xs="http://www.w3.org/2001/XMLSchema"
                    ID={index}
                    Version="2.0"
                    IssueInstant={now}>
      <saml:Issuer>
        {issuer}
      </saml:Issuer>
      <saml:Subject>
        <saml:NameID Format={instance.nameIDFormat}>{nameID}</saml:NameID>
        <saml:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
          <saml:SubjectConfirmationData
          InResponseTo={instance.requestId}
          Recipient={instance.url}
          NotOnOrAfter={(Instant.now().plusSeconds(300)).toString}/>
        </saml:SubjectConfirmation>
      </saml:Subject>
      <saml:Conditions NotBefore={(Instant.now().minusSeconds(300)).toString}
                       NotOnOrAfter={(Instant.now().plusSeconds(300)).toString}>
        <saml:AudienceRestriction>
          <saml:Audience>{audience}</saml:Audience>
        </saml:AudienceRestriction>
      </saml:Conditions>
      <saml:AuthnStatement AuthnInstant={now} SessionIndex={index}>
        <saml:AuthnContext>
          <saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml:AuthnContextClassRef>
        </saml:AuthnContext>
      </saml:AuthnStatement>
      <saml:AttributeStatement>
        {attributes.map {
        case (k, v) =>
          <saml:Attribute Name={k} NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:basic">
            <saml:AttributeValue xsi:type="xs:string">{v}</saml:AttributeValue>
          </saml:Attribute>
      }}
       <saml:Attribute Name="groups" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:basic">
        {groups.map {
          group => <saml:AttributeValue xsi:type="xs:string">{group}</saml:AttributeValue>
        }}
       </saml:Attribute>
      </saml:AttributeStatement>
    </saml:Assertion>

    //@formatter:on
  }
}
