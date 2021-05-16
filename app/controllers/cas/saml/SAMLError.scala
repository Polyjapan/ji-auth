package controllers.cas.saml

import ch.japanimpact.auth.api.cas.CASErrorType
import controllers.cas.CASv2Controller
import utils.RandomUtils

import java.time.Instant

object SAMLError {
  /**
   * @param responseId
   * @param instant
   * @return
   */
  object ErrorType extends Enumeration {
    type ErrorType = Value

    val VersionMismatch, Requester, Responder = Value
  }

  private def errorTypeToString(et: ErrorType.Value): String = "samlp:" + et.toString

  def apply(url: String, requestId: Option[String] = None, responseId: String = RandomUtils.randomString(160), instant: Instant = Instant.now())(errType: ErrorType.Value, casErr: Option[CASErrorType] = None, param: String = "") =
    <soapenv:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
      <soapenv:Header/>
      <soapenv:Body>
        <Response xmlns="urn:oasis:names:tc:SAML:1.0:protocol" xmlns:saml="urn:oasis:names:tc:SAML:1.0:assertion"
                  xmlns:samlp="urn:oasis:names:tc:SAML:1.0:protocol" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" IssueInstant={instant.toString}
                  MajorVersion="1" MinorVersion="1" Recipient={url}
                  ResponseID={responseId} InResponseTo={requestId.orNull}>
          <Status>
            <StatusCode Value={errorTypeToString(errType)}></StatusCode>
            {
              if (casErr.nonEmpty) {
                <StatusMessage>{casErr.get.message(param)}</StatusMessage>
              }
            }
          </Status>
        </Response>
      </soapenv:Body>
    </soapenv:Envelope>


}

/*

 */