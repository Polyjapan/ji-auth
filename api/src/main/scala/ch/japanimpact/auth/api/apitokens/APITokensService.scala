package ch.japanimpact.auth.api.apitokens

import ch.japanimpact.auth.api.ApiMapper
import ch.japanimpact.auth.api.constants.GeneralErrorCodes.ErrorCode
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{Json, Writes}
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}


/**
  * @author Louis Vialar
  */
@Singleton
class APITokensService @Inject()(ws: WSClient, config: Configuration)(implicit ec: ExecutionContext) {
  private val apiBase = config.get[String]("jiauth.baseUrl")
  private val apiSecret = config.getOptional[String]("jiauth.clientSecret").getOrElse("no_api_key")

  private def getEndpoint(req: TokenRequest): (String, WSRequest => WSRequest) = req match {
    case _: UserTokenRequest => ("user", r => r)
    case _: AppTokenRequest => ("app", _.addHttpHeaders("X-Client-Secret" -> apiSecret))
  }

  def getToken[C <: TokenRequest](req: C)(implicit writes: Writes[C]): Future[Either[ErrorResponse, TokenResponse]] = {
    val (endpoint, transformer) = getEndpoint(req)

    transformer(ws.url(s"$apiBase/api/token/$endpoint"))
      .post(Json.toJson(req))
      .map(_.json.as[Either[ErrorResponse, TokenResponse]])
  }
}


