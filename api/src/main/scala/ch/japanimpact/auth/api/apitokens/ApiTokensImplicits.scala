package ch.japanimpact.auth.api.apitokens

import java.time.Clock

import javax.inject.Inject
import play.api.Configuration
import play.api.mvc.RequestHeader

trait ApiTokensImplicits {
  implicit class RichRequestHeader @Inject()(request: RequestHeader)(implicit conf: Configuration, clock: Clock, jwt: APITokensValidationService) {
    def principal: Option[AuthentifiedPrincipal] = request.headers.get("Authorization").filter(_.startsWith("Bearer"))
      .map(_.replace("Bearer ", "").trim)
      .flatMap(jwt.validateToken)
  }
}
