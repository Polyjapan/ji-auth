package ch.japanimpact.auth.api.apitokens

import java.security.Security
import java.time.Clock

import ch.japanimpact.auth.api.PublicKeyLoader
import javax.inject.{Inject, Singleton}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import pdi.jwt.exceptions.JwtValidationException
import pdi.jwt.{JwtAlgorithm, JwtJson, JwtOptions}
import play.api.Configuration
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext
import scala.util.Try

@Singleton
class APITokensValidationService @Inject()(implicit executionContext: ExecutionContext, config: Configuration, clock: Clock, pk: PublicKeyLoader) {
  if (Security.getProvider("BC") == null) {
    Security.addProvider(new BouncyCastleProvider())
  }

  private lazy val audience: String = config.get[String]("jwt.audience")
  private lazy val issuer: String = config.getOptional[String]("jwt.issuer").getOrElse("auth")
  private val principalIdPattern = "^([a-z0-9.]+)\\|([0-9]+)$".r

  def decodeToken(token: String): Try[AuthentifiedPrincipal] =
    JwtJson.decode(token, pk.publicKey, JwtAlgorithm.allECDSA(), JwtOptions(signature = true, expiration = true, notBefore = true))
      .flatMap(claim => Try {
        // Check that the clain is not expired and has valid issuer+audience
        if (!claim.isValid(issuer, audience)) {
          throw new JwtValidationException("invalid token")
        }

        val scopes = (Json.parse(claim.content) \ "scopes").asOpt[Set[String]]

        (claim.subject, scopes) match {
          case (Some(principalIdPattern(tpe, id)), Some(scopes)) =>
            // will throw an exception if the tpe is not found or if id cannot be an int
            val principal = Principal.fromName(tpe)(id.toInt)
            AuthentifiedPrincipal(principal, scopes)

          case _ => throw new JwtValidationException("missing or invalid data in token")
        }
      })


  def validateToken(token: String): Option[AuthentifiedPrincipal] =
    decodeToken(token).toOption
}