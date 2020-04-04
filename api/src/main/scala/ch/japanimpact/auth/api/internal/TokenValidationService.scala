package ch.japanimpact.auth.api.internal

import ch.japanimpact.auth.api.{AuthorizedUser, PublicKeyLoader}
import javax.inject.{Inject, Singleton}
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.Configuration
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext
import scala.util.Try

@Singleton
class TokenValidationService @Inject()(implicit executionContext: ExecutionContext, config: Configuration, pk: PublicKeyLoader) {
  def decodeToken(token: String): Try[AuthorizedUser] =
    JwtJson.decode(token, pk.publicKey, JwtAlgorithm.allECDSA())
      .flatMap(claim => Try {
        val content = Json.parse(claim.content)
        AuthorizedUser(claim.subject.map(_.toInt).get, (content \ "grp").as[Set[String]])
      })


  def validateToken(token: String): Option[AuthorizedUser] =
    decodeToken(token).toOption
}