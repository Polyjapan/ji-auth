package controllers.api

import ch.japanimpact.auth.api.constants.GeneralErrorCodes.{InvalidAppSecret, MissingData}
import data.App
import models.AppsModel
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result, Results}
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
object ApiUtils {
  private[api] def withGivenApp(clientId: String, clientSecret: String)(body: App => Future[Result])(implicit apps: AppsModel, request: RequestHeader, ec: ExecutionContext): Future[Result] = {
    apps getAuthentifiedApp(clientId, clientSecret) flatMap {
      case Some(app) => body(app)
      case None => !InvalidAppSecret
    }
  }

  private[api] def withApp(body: App => Future[Result])(implicit apps: AppsModel, request: RequestHeader, ec: ExecutionContext): Future[Result] = {
    val h = request.headers

    if (!h.hasHeader("X-Client-Id") || !h.hasHeader("X-Client-Secret")) {
      Results.Unauthorized(Json.toJson(MissingData))
    } else {
      withGivenApp(h("X-Client-Id"), h("X-Client-Secret"))(body)
    }
  }

}
