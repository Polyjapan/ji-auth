package controllers.api.management

import java.time.Clock

import ch.japanimpact.api.APIError
import ch.japanimpact.auth.api.apitokens.AuthorizationActions
import ch.japanimpact.auth.api.apitokens.AuthorizationActions.OnlyUsers
import controllers.api.ApiUtils
import javax.inject.Inject
import models.ApiKeysModel
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class ApiKeysController @Inject()(cc: ControllerComponents, authorize: AuthorizationActions, apps: ApiKeysModel)(implicit ec: ExecutionContext, conf: Configuration, clock: Clock) extends AbstractController(cc) {


  def getApiKeys = authorize(OnlyUsers).async { req =>
    if (req.principal.hasScope("apikeys/get/all")) {
      apps.getApiKeys.map(lst => Ok(Json.toJson(lst.map(_.obfuscated))))
    } else if (req.principal.hasScope("apikeys/get/self")) {
      apps.getApiKeysByOwner(req.principal.principal.id).map(lst => Ok(Json.toJson(lst.map(_.obfuscated))))
    } else {
      Future.successful(Unauthorized)
    }
  }

  def getApiKey(id: Int) = authorize(OnlyUsers).async { req =>
    getApiKeyById(id)(req).map {
      case Some(app) => Ok(Json.toJson(app))
      case _ => NotFound
    }
  }

  case class CreateApiKeyRequest(appName: String)

  implicit val readsCreateApiKeyRequest = Json.reads[CreateApiKeyRequest]

  def createApiKey = authorize(OnlyUsers, "apikeys/create").async(parse.tolerantText(120)) { req =>
    apps.createApiKey(req.body, req.principal.principal.id)
      .map(key => Ok(Json.toJson(key)))
  }

  def updateApiKey(id: Int) = authorize(OnlyUsers).async(parse.tolerantText(120)) { req =>
    getApiKeyById(id, "apikeys/update/:user")(req).flatMap {
      case Some(app) =>
        apps.updateApiKey(app.apiKey.copy(appName = req.body)).map(_ => Ok)
      case _ => Future.successful(NotFound)
    }
  }

  def deleteApiKey(id: Int) = authorize(OnlyUsers).async { req =>
    getApiKeyById(id, "apikeys/delete/:user")(req).flatMap {
      case Some(app) =>
        apps.deleteApiKey(app.apiKey.appId.get).map(_ => Ok)
      case _ => Future.successful(NotFound)
    }
  }

  def getKeyScopes(id: Int) = authorize(OnlyUsers).async { req =>
    getApiKeyById(id, "apikeys/scopes/get/:user")(req).flatMap {
      case Some(app) =>
        apps.getAllowedScopes(app.apiKey.appId.get).map(set => Ok(Json.toJson(set)))
      case _ => Future.successful(NotFound)
    }
  }


  def addScope(id: Int) = authorize(OnlyUsers).async(parse.tolerantText(128)) { req =>
    if (!req.principal.hasScope(req.body))
      Future.successful(Forbidden(Json.toJson(APIError("forbidden", "You cannot give access to a scope you don't have access to."))))
    else if (!ApiUtils.ScopeRegex.matches(req.body))
      Future.successful(BadRequest(Json.toJson(APIError("invalid_scope", "The scope must match the following regex: ^[a-zA-Z0-9_*/-]{2,128}$"))))
    else
      getApiKeyById(id, "apikeys/scopes/add/:user")(req).flatMap {
        case Some(app) =>
          apps.addAllowedScope(app.apiKey.appId.get, req.body).map(_ => Ok)
        case _ => Future.successful(NotFound)
      }
  }

  def deleteScope(id: Int) = authorize(OnlyUsers).async(parse.tolerantText(128)) { req =>
    if (!req.principal.hasScope(req.body))
      Future.successful(Forbidden(Json.toJson(APIError("forbidden", "You cannot revoke access from a scope you don't have access to."))))
    else if (!ApiUtils.ScopeRegex.matches(req.body))
      Future.successful(BadRequest(Json.toJson(APIError("invalid_scope", "The scope must match the following regex: ^[a-zA-Z0-9_*/-]{2,128}$"))))
    else
      getApiKeyById(id, "apikeys/scopes/delete/:user")(req).flatMap {
        case Some(app) =>
          apps.removeAllowedScope(app.apiKey.appId.get, req.body).map(_ => Ok)
        case _ => Future.successful(NotFound)
      }
  }


  private def getApiKeyById(id: Int, permissionBase: String = "apikeys/get/:user")(req: authorize.AuthorizedRequest[_]): Future[Option[data.ApiKeyData]] =
    apps.getApiKeysById(id).map(_.filter {
      key =>
        if (req.principal.hasScope(permissionBase.replace(":user", "all")) ||
          req.principal.hasScope(permissionBase.replace(":user", s"${key.apiKey.appId.get}"))) {
          true
        } else if (req.principal.hasScope(permissionBase.replace(":user", "self"))) {
          key.apiKey.appCreatedBy == req.principal.principal.id
        } else {
          false
        }
    })
}
