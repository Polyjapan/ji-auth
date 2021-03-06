package controllers.api

import ch.japanimpact.auth.api.constants.GeneralErrorCodes.{InvalidAppSecret, MissingData, RequestError}
import data.ApiKey
import models.ApiKeysModel
import play.api.http.Writeable
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result, Results}
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
object ApiUtils {
  private[api] val ScopeRegex = "^[a-zA-Z0-9_/*-]{1,128}$".r

  private[api] def withGivenApp(clientSecret: String)(body: ApiKey => Future[Result])(implicit apps: ApiKeysModel, request: RequestHeader, ec: ExecutionContext): Future[Result] = {
    apps getApiKeyBySecret (clientSecret) flatMap {
      case Some(app) => body(app)
      case None => !InvalidAppSecret
    }
  }

  private[api] def withApp(body: ApiKey => Future[Result])(implicit apps: ApiKeysModel, request: RequestHeader, ec: ExecutionContext): Future[Result] = {
    withApp(Json.toJson[RequestError](MissingData))(body)
  }

  private[api] def withApp[C](errorToReturn: => C)(body: ApiKey => Future[Result])(implicit apps: ApiKeysModel, request: RequestHeader, ec: ExecutionContext, w: Writeable[C]): Future[Result] = {
    val h = request.headers

    if (!h.hasHeader("X-Client-Secret")) {
      Results.Unauthorized(errorToReturn)
    } else {
      withGivenApp(h("X-Client-Secret"))(body)
    }
  }

}
