package services

import java.security.{PrivateKey, PublicKey}

import com.google.inject.Inject
import javax.inject.Singleton
import pdi.jwt.algorithms.JwtAsymetricAlgorithm
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtJson}
import play.api.Configuration
import play.api.libs.json.JsObject

/**
  * @author Louis Vialar
  */
@Singleton
class JwtService @Inject()(conf: Configuration) {

  private lazy val pubKeyPath = conf.get[String]("jwt.publicKeyPath")
  private lazy val privKeyPath = conf.get[String]("jwt.privateKeyPath")
  private lazy val algoName = conf.getOptional[String]("jwt.algo").getOrElse("RS512")

  private val (privKey: PrivateKey, pubKey: PublicKey) = readKeys()
  private val algo = JwtAlgorithm.fromString(algoName).asInstanceOf[JwtAsymetricAlgorithm]

  private def readKeys(): (PrivateKey, PublicKey) = {
    import java.nio.file.{Files, Paths}
    import java.security.KeyFactory
    import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
    import java.util.Base64

    val privateKeyContent = new String(Files.readAllBytes(Paths.get(privKeyPath)))
      .replaceAll("\\n", "").replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")

    val publicKeyContent = new String(Files.readAllBytes(Paths.get(pubKeyPath)))
      .replaceAll("\\n", "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")

    val kf = KeyFactory.getInstance("RSA")

    val keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder.decode(privateKeyContent))
    val privKey = kf.generatePrivate(keySpecPKCS8)

    val keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder.decode(publicKeyContent))
    val pubKey = kf.generatePublic(keySpecX509)

    (privKey, pubKey)
  }

  def encodeJson(body: JsObject): String = {
    JwtJson.encode(body, privKey, algo)
  }

  def encodeJwt(body: JwtClaim): String = {
    Jwt.encode(body, privKey, algo)
  }

  def checkJwt(jwt: String) = {
    JwtJson.decode(jwt, pubKey, Seq(algo))
  }

}
