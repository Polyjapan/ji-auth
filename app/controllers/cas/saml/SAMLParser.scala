package controllers.cas.saml

import java.time.Instant
import scala.xml.NodeSeq

object SAMLParser {
  case class SamlValidateRequest(requestId: String, timestamp: Instant, serviceTicket: String)

  case class IllegalVersionException() extends Exception
  case class InvalidRequestException() extends Exception

  @throws[IllegalVersionException]
  @throws[InvalidRequestException]
  def apply(x: NodeSeq): SamlValidateRequest = {
    x.collectFirst { case x: xml.Elem if x.label == "Envelope" => x}
      .flatMap(root => (root \ "Body").headOption)
      .flatMap(soap => (soap \ "samlp:Request").headOption)
      .flatMap { rq =>

        val majorVersion = rq \@ "MajorVersion"
        val minorVersion = rq \@ "MajorVersion"

        if (majorVersion != "1" || minorVersion != "1") {
          // we don't support SAMLv2
          throw IllegalVersionException()
        } else {
          val rqId = rq \@ "RequestID"
          val token = (rq \ "samlp:AssertionArtifact").headOption.map(_.text)

          val timestamp = rq \@ "IssueInstant"

          token map (token => SamlValidateRequest(rqId, Instant.parse(timestamp), token))
        }
      } match {
      case Some(rq) => rq
      case None => throw InvalidRequestException()
    }
  }
}
