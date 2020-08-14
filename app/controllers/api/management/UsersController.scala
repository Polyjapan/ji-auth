package controllers.api.management

import java.time.Clock

import ch.japanimpact.api.{APIError, APIErrorsHelper}
import ch.japanimpact.auth.api.UserProfile
import ch.japanimpact.auth.api.apitokens.AuthorizationActions
import controllers.api.ApiUtils
import javax.inject.Inject
import models.{GroupsModel, SessionsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class UsersController @Inject()(cc: ControllerComponents, authorize: AuthorizationActions, users: UsersModel,
                                groups: GroupsModel, tickets: TicketsModel, sessions: SessionsModel)(implicit ec: ExecutionContext, conf: Configuration, clock: Clock) extends AbstractController(cc) with APIErrorsHelper {
  def getUsers = authorize("users/list").async { req =>
    users.getUsers.map(lst => Ok(Json.toJson(lst)))
  }

  def getUser(id: Int) = authorize("users/get").async { req =>
    users.getUserData(id).map {
      case Some(data) => Ok(Json.toJson(data))
      case None => APIError(NotFound, "not_found", s"No user with id $id")
    }
  }

  def searchUsers(query: String) = authorize("users/search").async { implicit rq =>
    // Sounds legacy, maybe we want to search full user data?
    users.searchUsers(query).map(
      seq => seq.map(user => {
        val details = user.toUserDetails
        UserProfile(user.id.get, user.email, details, None)
      })).map(r => Ok(Json.toJson(r)))
  }

  def getUsersWithIds(idsStr: String) = authorize("users/get").async { implicit rq =>
    val ids = idsStr.split(",").filter(_.forall(_.isDigit)).map(_.toInt).toSet

    users.getUsersData(ids)
      .map { data =>
        Ok(Json.toJson(data.map { case (id, data) => (id.toString, data) }))
      }
  }

  def forceLogOut(id: Int) = authorize("users/forceLogout").async { req =>
    tickets.logout(id).flatMap(_ => sessions.logoutUser(id)).map(_ => Ok)
  }

  def forceConfirmEmail(id: Int) = authorize("users/forceEmailConfirm").async { req =>
    users.getUserById(id) flatMap {
      case None => Future.successful(APIError(NotFound, "not_found", s"No user with id $id"))
      case Some(user) => users.updateUser(user.copy(emailConfirmKey = None)).map(_ => Ok)
    }
  }

  def addScope(id: Int) = authorize(s"users/scopes/add").async(parse.tolerantText(128)) { req =>
    if (!req.principal.hasScope(req.body))
      Future.successful(APIError(Forbidden, "forbidden", "You cannot give access to a scope you don't have access to."))
    else if (!ApiUtils.ScopeRegex.matches(req.body))
      Future.successful(APIError(BadRequest, "invalid_scope", "The scope must match the following regex: ^[a-zA-Z0-9_*/-]{2,128}$"))
    else
      users.addAllowedScope(id, req.body).map(_ => Ok)
  }

  def deleteScope(id: Int) = authorize(s"users/scopes/delete").async(parse.tolerantText(128)) { req =>
    if (!req.principal.hasScope(req.body))
      Future.successful(APIError(Forbidden, "forbidden", "You cannot revoke access from a scope you don't have access to."))
    else if (!ApiUtils.ScopeRegex.matches(req.body))
      Future.successful(APIError(BadRequest, "invalid_scope", "The scope must match the following regex: ^[a-zA-Z0-9_*/-]{2,128}$"))
    else
      users.removeAllowedScope(id, req.body).map(_ => Ok)
  }

  def updateUser(id: Int) = authorize("users/update").async(parse.json[UserProfile]) { req =>
    users.getUserById(id) flatMap {
      case None => Future.successful(APIError(NotFound, "not_found", s"No user with id $id"))
      case Some(user) =>
        users.updateUser(
          user.copy(
            email = req.body.email,
            firstName = req.body.details.firstName,
            lastName = req.body.details.lastName,
            phoneNumber = req.body.details.phoneNumber
          )
        ).map(_ => Ok)
    }
  }

}
