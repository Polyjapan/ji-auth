package controllers.cas.saml

import controllers.cas.CASv2Controller

import java.time.Instant

object SAMLSuccess {
  /**
   * @param responseId
   * @param instant
   * @return
   */
  def apply(url: String, responseId: String, requestId: String, service: String, instant: Instant, properties: Map[String, String], attributes: Map[String, String], groups: Set[String], issuer: String = "auth") = {
    val subject = {
      <Subject>
        <NameIdentifier>
          {properties(CASv2Controller.UserProperty)}
        </NameIdentifier>
        <SubjectConfirmation>
          <ConfirmationMethod>
            urn:oasis:names:tc:SAML:1.0:cm:artifact
          </ConfirmationMethod>
        </SubjectConfirmation>
      </Subject>
    }

    <soapenv:Envelope>
      <soapenv:Header/>
      <soapenv:Body>
        <Response xmlns="urn:oasis:names:tc:SAML:1.0:protocol" xmlns:saml="urn:oasis:names:tc:SAML:1.0:assertion"
                  xmlns:samlp="urn:oasis:names:tc:SAML:1.0:protocol" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" IssueInstant={instant.toString}
                  MajorVersion="1" MinorVersion="1" Recipient={url}
                  ResponseID={responseId} InResponseTo={requestId}>
          <Status>
            <StatusCode Value="samlp:Success"></StatusCode>
          </Status>
          <Assertion xmlns="urn:oasis:names:tc:SAML:1.0:assertion" AssertionID="_e5c23ff7a3889e12fa01802a47331653"
                     IssueInstant={instant.toString} Issuer={issuer} MajorVersion="1"
                     MinorVersion="1">
            <Conditions NotBefore={instant.toString} NotOnOrAfter={instant.plusSeconds(30).toString}>
              <AudienceRestrictionCondition>
                <Audience>
                  {service}
                </Audience>
              </AudienceRestrictionCondition>
            </Conditions>
            <AttributeStatement>
              {subject}{attributes.map(attr => {
              val (k, v) = attr
              <Attribute AttributeName={k} AttributeNamespace="http://www.ja-sig.org/products/cas/">
                <AttributeValue>
                  {v}
                </AttributeValue>
              </Attribute>
            })}<Attribute AttributeName="groups" AttributeNamespace="http://www.ja-sig.org/products/cas/">
              {groups.map(gr => <AttributeValue>
                {gr}
              </AttributeValue>)}
            </Attribute>
            </AttributeStatement>
            <AuthenticationStatement AuthenticationInstant={instant.toString}
                                     AuthenticationMethod="urn:oasis:names:tc:SAML:1.0:am:password">

              {subject}
            </AuthenticationStatement>
          </Assertion>
        </Response>
      </soapenv:Body>
    </soapenv:Envelope>
  }


}

/*

 */