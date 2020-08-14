package ch.japanimpact.auth.api

import akka.Done
import ch.japanimpact.api.APIError
import ch.japanimpact.auth.api.apitokens.{APITokensService, AppTokenRequest}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.cache.AsyncCacheApi
import play.api.libs.json.JsValue
import play.api.libs.ws._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


/**
 * @author Louis Vialar
 */
abstract class AbstractHttpApi(scopes: Set[String], ws: WSClient, config: Configuration, tokens: APITokensService, cache: AsyncCacheApi)(implicit ec: ExecutionContext) {
  private val apiBase: String = config.getOptional[String]("jiauth.api.baseUrl").getOrElse(config.get[String]("jiauth.baseUrl") + "/api/v2/")
  private val cacheDuration = config.getOptional[Duration]("jiauth.cacheDuration").getOrElse(10.minutes)
  private val tokenDuration = config.getOptional[Duration]("jiauth.tokenDuration").getOrElse(48.hours)
  private val token = new TokenHolder

  protected def withToken[T](endpoint: String)(exec: WSRequest => Future[WSResponse])(map: JsValue => T): Future[Either[APIError, T]] =
    token()
      .map(token => ws.url(s"$apiBase/$endpoint").addHttpHeaders("Authorization" -> ("Bearer " + token)))
      .flatMap(r => mapping(r)(exec)(map))
      .recover {
        case ex: Throwable => Left(APIError(ex.getClass.getName, ex.getMessage))
      }

  protected def withTokenToDone(endpoint: String)(exec: WSRequest => Future[WSResponse]): Future[Either[APIError, Done]] =
    token()
      .map(token => ws.url(s"$apiBase/$endpoint").addHttpHeaders("Authorization" -> ("Bearer " + token)))
      .flatMap(r => {
        exec(r).map { resp =>
          if (resp.status == 200) Right(Done)
          else {
            Left(resp.json.as[APIError])
          }
        }.recover {
          case ex: Throwable => Left(APIError(ex.getClass.getName, ex.getMessage))
        }
      })
      .recover {
        case ex: Throwable => Left(APIError(ex.getClass.getName, ex.getMessage))
      }

  protected def mapping[T](request: WSRequest)(exec: WSRequest => Future[WSResponse])(map: JsValue => T): Future[Either[APIError, T]] = {
    val r = exec(request)

    r.map { response =>

      if (response.status == 200) {
        try {
          Right(map(response.json))
        } catch {
          case e: Exception =>
            e.printStackTrace()
            println(response.body)
            Left(APIError("unknown_error", "Unknown error with success response mapping"))
        }
      } else {
        try {
          Left(response.json.as[APIError])
        } catch {
          case e: Exception =>
            e.printStackTrace()
            Left(APIError("unknown_error", "Unknown error with code " + response.status))
        }
      }

    }.recover {
      case ex: Throwable => Left(APIError(ex.getClass.getName, ex.getMessage))
    }
  }

  protected def cacheOnlySuccess[T](cacheKey: String)(orElse: => Future[Either[APIError, T]]): Future[Either[APIError, T]] = {
    cache.getOrElseUpdate(cacheKey, cacheDuration)(orElse)
      .map { o =>
        if (o.isLeft) cache.remove(cacheKey)
        o
      }

  }

  private class TokenHolder {
    var token: String = _
    var exp: Long = _

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

}


