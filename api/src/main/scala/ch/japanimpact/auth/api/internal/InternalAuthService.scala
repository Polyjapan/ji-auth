package ch.japanimpact.auth.api.internal

import java.security.Security

import ch.japanimpact.auth.api.{ApiMapper, AuthApi}
import ch.japanimpact.auth.api.constants.GeneralErrorCodes.ErrorCode
import javax.inject.{Inject, Singleton}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import play.api.Configuration
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}


/**
  * @author Louis Vialar
  */
@Singleton
class InternalAuthService(val ws: WSClient, val apiBase: String)(implicit ec: ExecutionContext) extends ApiMapper {
  def refreshToken(refreshToken: String): Future[Either[TokenResponse, ErrorCode]] = {
    ws.url(apiBase + "/api/refresh/" + refreshToken)
      .get()
      .map(mapResponseToEither[TokenResponse](""))
  }
}


object InternalAuthService {
  @Inject()
  def apply(client: WSClient)(implicit executionContext: ExecutionContext, config: Configuration): InternalAuthService = {
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider())
    }

    val apiRoot: String = config.get[String]("jiauth.baseUrl")

    new InternalAuthService(client, apiRoot)
  }
}