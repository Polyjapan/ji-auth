package controllers.saml2

import services.HashService

import java.time.Instant


class SAMLv2ParserTest extends org.specs2.mutable.Specification {

  "SAMLv2Parser" should {
    "parse basic requests correctly" in {
      val id = "_d4ad1e160780993e9bc1"
      val instant = Instant.now()
      val acs = "https://test.japan-impact.ch/auth/saml/callback"
      val dest = "https://auth.japan-impact.ch/SAML2/SSO/Redirect"
      val iss = "https://hedgedoc.japan-impact.ch"
      val binding = SAMLBindings.HTTPPost
      val format = SAMLNameIdFormats.EmailAddressFormat

      val req =
        <samlp:AuthnRequest
        AssertionConsumerServiceURL={acs}
        Destination={dest}
        ProtocolBinding={binding}
        IssueInstant={instant.toString}
        Version="2.0" ID={id}
        xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol">
          <saml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">{iss}</saml:Issuer>
          <samlp:NameIDPolicy AllowCreate="true" Format={format}
                              xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"/>
          <samlp:RequestedAuthnContext Comparison="exact" xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol">
            <saml:AuthnContextClassRef xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml:AuthnContextClassRef>
          </samlp:RequestedAuthnContext>
        </samlp:AuthnRequest>

      val parsed = SAMLv2Parser.parseAuthnRequest(req)

      parsed.requestId shouldEqual id
      parsed.issueInstant shouldEqual instant
      parsed.assertionConsumerServiceURL shouldEqual Some(acs)
      parsed.destination shouldEqual Some(dest)
      parsed.issuer shouldEqual Some(iss)
      parsed.protocolBinding shouldEqual Some(binding)
      parsed.nameIdPolicy.flatMap(_.format) shouldEqual Some(format)
    }

  }
}
