package services

import com.onelogin.saml2.util.{Constants => OneLoginConstants, Util => SAMLUtils}
import org.w3c.dom.Document
import play.api.Configuration
import utils.KeyUtils
import utils.KeyUtils.KeyTypes

import java.nio.file.{Files, Paths}
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.inject.{Inject, Singleton}

@Singleton
class XMLSignService @Inject()(conf: Configuration) {

  /**
   * The private key to sign JWT or SAML responses
   */
  private lazy val privateKey: PrivateKey = KeyUtils.parsePrivateKey(privateKeyData, KeyTypes.RSA)
  private lazy val certificate: X509Certificate = KeyUtils.parseX509Certificate(certData)

  private lazy val privateKeyData = new String(Files.readAllBytes(Paths.get(conf.get[String]("saml.keypair.private"))))
  private lazy val certData = new String(Files.readAllBytes(Paths.get(conf.get[String]("saml.keypair.cert"))))

  private val DigestAlgo = OneLoginConstants.SHA256
  private val SignatureAlgo = OneLoginConstants.RSA_SHA256

  def signScalaElement(elem: xml.Elem): String =
    samlSign(SAMLUtils.loadXML(elem.toString()))

  def samlSign(xmlDoc: Document): String = {
    val ret = SAMLUtils.addSign(xmlDoc, privateKey, certificate, SignatureAlgo, DigestAlgo)
    SAMLUtils.base64encoder(ret)
  }

}
