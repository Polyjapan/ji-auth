package ch.japanimpact.auth.api

import akka.Done
import ch.japanimpact.api.APIError
import ch.japanimpact.auth.api.apitokens.{APITokensService, AppTokenRequest, TokenHolder}

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
abstract class AbstractHttpApi(scopes: Set[String], ws: WSClient, config: Configuration, cache: AsyncCacheApi)(implicit ec: ExecutionContext, tokens: APITokensService) {
  private val apiBase: String = config.getOptional[String]("jiauth.api.baseUrl").getOrElse({
    val base = config.get[String]("jiauth.baseUrl")
    var url = (if (base.endsWith("/")) base else base + "/") + "api/v2/"
    while (url.endsWith("/")) url = url.dropRight(1)
    url
  })

  private val cacheDuration = config.getOptional[Duration]("jiauth.cacheDuration").getOrElse(10.minutes)
  private val tokenDuration = config.getOptional[Duration]("jiauth.tokenDuration").getOrElse(48.hours)
  private val token = new TokenHolder(scopes, Set("auth"), tokenDuration)

  protected def withToken[T](endpoint: String)(exec: WSRequest => Future[WSResponse])(map: JsValue => T): Future[Either[APIError, T]] =
    token()
      .map(token => ws.url(s"$apiBase/${endpoint.dropWhile(_ == '/')}").addHttpHeaders("Authorization" -> ("Bearer " + token)))
      .flatMap(r => mapping(r)(exec)(map))
      .recover {
        case ex: Throwable => Left(APIError(ex.getClass.getName, ex.getMessage))
      }

  protected def withTokenToDone(endpoint: String)(exec: WSRequest => Future[WSResponse]): Future[Either[APIError, Done]] =
    token()
      .map(token => ws.url(s"$apiBase/${endpoint.dropWhile(_ == '/')}").addHttpHeaders("Authorization" -> ("Bearer " + token)))
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


}


