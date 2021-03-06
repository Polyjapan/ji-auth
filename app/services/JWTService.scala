package services

import java.nio.file.{Files, Paths}
import java.time.Clock

import ch.japanimpact.auth.api.apitokens.Principal
import data.SessionID
import javax.inject.{Inject, Singleton}
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtJson}
import play.api.Configuration
import play.api.libs.json.Json

import scala.concurrent.duration._

@Singleton
class JWTService @Inject()(conf: Configuration) {
  private implicit val clock: Clock = Clock.systemUTC
  val ExpirationTimeMinutes: Int = conf.getOptional[Int]("jwt.expirationTime").getOrElse(10)
  private lazy val privKeyPath: String = conf.get[String]("jwt.privateKeyPath")

  private lazy val privateKey = new String(Files.readAllBytes(Paths.get(privKeyPath)))

  // The ticket must be as light as possible
  // User data is not needed here, we only want uid and groups!

  def issueInternalToken(userId: Int, groups: Set[String]): String = {
    val claim = JwtClaim(Json.obj(
      "grp" -> groups
    ).toString())
      .about(userId.toString)
      .to("internal")
      .issuedNow
      .expiresIn(ExpirationTimeMinutes * 60)


    JwtJson.encode(claim, privateKey, JwtAlgorithm.ES256)
  }

  /**
   * Create a new API token, usable by internal APIs
   * @param principal
   * @param scopes
   * @param services
   * @param duration
   * @return
   */
  def issueApiToken(principal: Principal, scopes: Set[String], services: Set[String], duration: Duration): (Long, String) = {
    val time = if (duration.toHours > 48) 48 hours else duration

    val claim = JwtClaim(Json.obj(
      "scopes" -> scopes
    ).toString())
      .about(principal.toSubject)
      .by("auth")
      .to(services)
      .issuedNow
      .expiresIn(time.toSeconds)


    (time.toSeconds, JwtJson.encode(claim, privateKey, JwtAlgorithm.ES256))
  }

}
