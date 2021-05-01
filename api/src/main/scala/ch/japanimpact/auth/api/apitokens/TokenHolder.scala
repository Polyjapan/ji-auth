package ch.japanimpact.auth.api.apitokens

import javax.inject.Singleton
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
 * This class requests API tokens and keeps them while they are valid.
 *
 * Example usage:
 *
 * val tokens = new TokenHolder(Set("api/blah", Set("auth"), 12.hours)
 * val req = token().map(token => my_request_requiring_a_token_here)
 *
 * @param scopes        the scopes required in the requested token
 * @param audiences     the audiences targeted for the token
 * @param tokenDuration the interval between token renewals
 */
class TokenHolder(scopes: Set[String], audiences: Set[String], tokenDuration: Duration)(implicit tokens: APITokensService, ec: ExecutionContext) {
  private var token: String = _
  private var exp: Long = _

  def apply(): Future[String] = {
    if (token != null && exp > System.currentTimeMillis() + 1000) Future.successful(token)
    else {
      tokens.getToken(AppTokenRequest(scopes, Set("auth"), tokenDuration.toSeconds))
        .map {
          case Right(token) =>
            this.token = token.token
            this.exp = System.currentTimeMillis() + token.duration * 1000 - 1000

            this.token
          case _ => throw new Exception("No token returned")
        }
    }
  }
}