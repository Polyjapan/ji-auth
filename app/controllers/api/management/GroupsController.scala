package controllers.api.management

import java.time.Clock

import ch.japanimpact.api.APIError
import ch.japanimpact.auth.api.Group
import ch.japanimpact.auth.api.apitokens.AuthorizationActions
import controllers.api.ApiUtils
import javax.inject.Inject
import models.GroupsModel
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class GroupsController @Inject()(cc: ControllerComponents, authorize: AuthorizationActions, groups: GroupsModel)(implicit ec: ExecutionContext, conf: Configuration, clock: Clock) extends AbstractController(cc) {
  private val namingRegex = "^[a-zA-Z0-9_-]{2,128}$".r

  def getGroups = authorize("groups/list").async { req =>
    groups.getAllGroups.map(lst => Ok(Json.toJson(lst)))
  }

  def createGroup = authorize("groups/create").async(parse.json[Group]) { req =>
    if (!namingRegex.matches(req.body.name)) {
      Future.successful(BadRequest(Json.toJson(APIError("invalid_name", "The group name must match the following regex: ^[a-zA-Z0-9_-]{2,128}$"))))
    } else {
      groups.createGroup(req.body).map(res => Ok(Json.toJson(res)))
    }
  }

  def updateGroup(name: String) = authorize(s"groups/modify/$name").async(parse.json[Group]) { req =>
    if (!namingRegex.matches(req.body.name)) {
      Future.successful(BadRequest(Json.toJson(APIError("invalid_name", "The group name must match the following regex: ^[a-zA-Z0-9_-]{2,128}$"))))
    } else {
      groups.updateGroup(name, req.body).map(res => Ok)
    }
  }

  def getGroup(name: String) = authorize(s"groups/get/$name").async { req =>
    groups.getGroup(name).map {
      case Some(lst) => Ok(Json.toJson(lst))
      case None => NotFound
    }
  }

  def deleteGroup(name: String) = authorize(s"groups/delete/$name").async { req =>
    groups.deleteGroup(name).map(_ => Ok)
  }

  def addScope(name: String) = authorize(s"groups/scopes/add/$name").async(parse.tolerantText(128)) { req =>
    if (!req.principal.hasScope(req.body))
      Future.successful(Forbidden(Json.toJson(APIError("forbidden", "You cannot give access to a scope you don't have access to."))))
    else if (!ApiUtils.ScopeRegex.matches(req.body))
      Future.successful(BadRequest(Json.toJson(APIError("invalid_scope", "The scope must match the following regex: ^[a-zA-Z0-9_*/-]{2,128}$"))))
    else
      groups.addScope(name, req.body).map(_ => Ok)
  }

  def deleteScope(name: String) = authorize(s"groups/scopes/delete/$name").async(parse.tolerantText(128)) { req =>
    if (!req.principal.hasScope(req.body))
      Future.successful(Forbidden(Json.toJson(APIError("forbidden", "You cannot revoke access from a scope you don't have access to."))))
    else if (!ApiUtils.ScopeRegex.matches(req.body))
      Future.successful(BadRequest(Json.toJson(APIError("invalid_scope", "The scope must match the following regex: ^[a-zA-Z0-9_*/-]{2,128}$"))))
    else
      groups.removeScope(name, req.body).map(_ => Ok)
  }

  def addMember(name: String) = authorize(s"groups/members/add/$name").async(parse.json[Int]) { req =>
    groups.addMember(name, req.body).map(_ => Ok)
  }

  def deleteMember(name: String) = authorize(s"groups/members/delete/$name").async(parse.json[Int]) { req =>
    groups.removeMember(name, req.body).map(_ => Ok)
  }

  def getMembers(name: String) = authorize(s"groups/members/list/$name").async { req =>
    groups.getGroupMembers(name).map(lst => Ok(Json.toJson(lst)))
  }

}
