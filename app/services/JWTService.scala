package services

import java.nio.file.{Files, Paths}
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{KeyFactory, PrivateKey}
import java.time.Clock
import java.util.{Base64, UUID}

import javax.inject.{Inject, Singleton}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtJson}
import play.api.Configuration
import play.api.libs.json.Json
import data.SessionID

@Singleton
class JWTService @Inject()(conf: Configuration) {
  private implicit val clock: Clock = Clock.systemUTC
  private val ExpirationTimeMinutes: Int = conf.getOptional[Int]("jwt.expirationTime").getOrElse(10)
  private lazy val privKeyPath: String = conf.get[String]("jwt.privateKeyPath")

  private lazy val privateKey = new String(Files.readAllBytes(Paths.get(privKeyPath)))

  // The ticket must be as light as possible
  // User data is not needed here, we only want uid and groups!

  def issueToken(sessionId: SessionID, userId: Int, groups: Set[String]): String = {
    val claim = JwtClaim(Json.obj(
      "grp" -> groups, "sid" -> sessionId.toString
    ).toString())
      .issuedNow
      .expiresIn(ExpirationTimeMinutes * 60)


    JwtJson.encode(claim, privateKey, JwtAlgorithm.ES256)
  }

}
