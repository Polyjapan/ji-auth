package utils

import java.io.ByteArrayInputStream
import java.security.cert.{Certificate, CertificateFactory, X509Certificate}
import java.security.{KeyFactory, PrivateKey, PublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util.Base64

object KeyUtils {
  object KeyTypes {
    abstract class KeyType(val algoName: String)
    abstract class CertType(val algoName: String)

    object ECDSA extends KeyType("EC")
    object RSA extends KeyType("RSA")

    object X509 extends CertType("X.509")
  }

  private def keyToBytes(key: String) =
    Base64.getDecoder.decode(
      key.replaceAll("-----BEGIN (.*)-----", "")
        .replaceAll("-----END (.*)-----", "")
        .replaceAll("\r\n", "")
        .replaceAll("\n", "")
        .trim
    )

  def parsePrivateKey(pk: String, kind: KeyTypes.KeyType): PrivateKey = {
    val spec = new PKCS8EncodedKeySpec(keyToBytes(pk))
    KeyFactory.getInstance(kind.algoName).generatePrivate(spec)
  }

  def parsePublicKey(pk: String, kind: KeyTypes.KeyType): PublicKey = {
    val spec = new X509EncodedKeySpec(keyToBytes(pk))
    KeyFactory.getInstance(kind.algoName).generatePublic(spec)
  }

  def parseX509Certificate(cert: String): X509Certificate = {
    CertificateFactory.getInstance("X.509")
      .generateCertificate(new ByteArrayInputStream(cert.getBytes))
      .asInstanceOf[X509Certificate]
  }

}
