package ch.japanimpact.auth.api

import java.security.Security

import ch.japanimpact.auth.api.constants.GeneralErrorCodes
import ch.japanimpact.auth.api.constants.GeneralErrorCodes.{ErrorCode, RequestError}
import javax.inject.{Inject, Singleton}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


/**
  * @author Louis Vialar
  */
@Singleton
class AuthApi(val ws: WSClient, val apiBase: String, val apiClientSecret: String)(implicit ec: ExecutionContext) extends ApiMapper {

  private val reg = "^[0-9a-zA-Z_=-]{60,120}$".r

  @deprecated
  def isValidTicket(ticket: String): Boolean =
    reg.findFirstIn(ticket).nonEmpty

  implicit class AuthWSRequest(rq: WSRequest) {
    def asCurrentApp: WSRequest = rq.addHttpHeaders(
      "X-Client-Secret" -> apiClientSecret,
    )

    def asUserWithToken(token: String): WSRequest = rq.addHttpHeaders(
      "Authorization" -> ("Bearer " + token)
    )
  }

  /**
    * Search users in Auth
    *
    * @param query the query string to look for
    * @return the users
    */
  def searchUser(query: String): Future[List[UserProfile]] = {
    if (query.length < 3)
      throw new IllegalArgumentException("invalid query")

    ws.url(apiBase + "/api/user/search/" + query)
      .asCurrentApp
      .get()
      .map(r => {
        r.json.as[List[UserProfile]]
      })
  }

  /**
    * Gets the profile of a user. If the user didn't migrate his account to add his profile information, this will not return.
    *
    * @param userId the id of the user to return
    * @return either the profile or an error
    *         if the credentials are invalid, a [[GeneralErrorCodes.InvalidAppSecret]] error is returned
    *         if the user doesn't exist, a [[GeneralErrorCodes.UserNotFound]] error is returned
    *         a [[GeneralErrorCodes.UnknownError]] might be produced if something happens
    */
  def getUserProfile(userId: Int): Future[Either[UserProfile, ErrorCode]] = {
    ws.url(apiBase + "/api/user/" + userId)
      .asCurrentApp
      .get()
      .map(mapResponseToEither[UserProfile](""))
  }


  /**
    * Gets the profile of a user. If the user didn't migrate his account to add his profile information, this will not return.
    *
    * @param token the session ID or jwt token of the user to return
    * @return either the profile or an error
    *         if the credentials are invalid, a [[GeneralErrorCodes.InvalidAppSecret]] error is returned
    *         if the user doesn't exist, a [[GeneralErrorCodes.UserNotFound]] error is returned
    *         a [[GeneralErrorCodes.UnknownError]] might be produced if something happens
    */
  def getUserProfileByToken(token: String): Future[Either[UserProfile, ErrorCode]] = {
    ws.url(apiBase + "/api/user")
      .asUserWithToken(token)
      .get()
      .map(mapResponseToEither[UserProfile](""))
  }

  /**
    * Gets the profile of a user. If the user didn't migrate his account to add his profile information, this will not return.
    *
    * @param userIds the set of ids of the users to return
    * @return either the profile or an error
    *         if the credentials are invalid, a [[GeneralErrorCodes.InvalidAppSecret]] error is returned
    *         if the user doesn't exist, a [[GeneralErrorCodes.UserNotFound]] error is returned
    *         a [[GeneralErrorCodes.UnknownError]] might be produced if something happens
    */
  def getUserProfiles(userIds: Set[Int]): Future[Either[Map[Int, UserProfile], ErrorCode]] = {
    if (userIds.size > 300) {
      val (left, right) = userIds.splitAt(300) // Split the request

      val l = getUserProfiles(left)
      val r = getUserProfiles(right)

      Future.reduceLeft(List(l, r)) {
        case (Left(lMap), Left(rMap)) => Left(lMap ++ rMap)
        case (Right(lErr), _) => Right(lErr)
        case (_, Right(rErr)) => Right(rErr)
      }
    } else if (userIds.nonEmpty) {
      ws.url(apiBase + "/api/users/" + userIds.mkString(","))
        .asCurrentApp
        .get()
        .map(mapResponseToEither[Map[String, UserProfile]]("get_ticket"))
        .map(_.left.map { map => map.map(pair => (pair._1.toInt, pair._2))})
    } else {
      Future.successful(Left(Map()))
    }
  }

  /**
    * Add an user to a group. The group must be a group in which the app owner has a write access.
    *
    * @param group  the identifier of the group
    * @param userId the user to add to the group
    * @return an option, holding nothing if everything went fine, or an error otherwise.
    *         if the credentials are invalid, a [[GeneralErrorCodes.InvalidAppSecret]] error is returned
    *         if the group doesn't exist, a [[GeneralErrorCodes.GroupNotFound]] error is returned
    *         if you can't add members to the group, a [[GeneralErrorCodes.MissingPermission]] error is returned
    *         a [[GeneralErrorCodes.UnknownError]] might be produced if something happens
    */
  def addUserToGroup(group: String, userId: Int): Future[Option[ErrorCode]] = {
    ws.url(apiBase + "/api/groups/" + group + "/members")
      .asCurrentApp
      .post(Json.toJson("userId" -> userId))
      .map(mapResponse("group_add_user", _ => None, e => Some(e)))
  }
  /**
    * Get all the users of a group. The group must be a group in which the app owner has a read access.
    *
    * @param group  the identifier of the group
    * @return an option, holding nothing if everything went fine, or an error otherwise.
    *         if the credentials are invalid, a [[GeneralErrorCodes.InvalidAppSecret]] error is returned
    *         if the group doesn't exist, a [[GeneralErrorCodes.GroupNotFound]] error is returned
    *         if you can't read members of the group, a [[GeneralErrorCodes.MissingPermission]] error is returned
    *         a [[GeneralErrorCodes.UnknownError]] might be produced if something happens
    */
  def getGroupMembers(group: String): Future[Either[List[UserProfile], ErrorCode]] = {
    ws.url(apiBase + "/api/groups/" + group + "/members")
      .asCurrentApp
      .get
      .map(mapResponseToEither[List[UserProfile]]("group_get_users"))
  }

  /**
    * Remove an user from a group. The group must be a group in which the app owner has a write access. The app owner
    * must also have the permissions to remove this user. (If the user is the group owner, this operation will always
    * fail. Same thing if the user is a group admin but the app owner isn't)
    *
    * @param group  the identifier of the group
    * @param userId the user to add to the group
    * @return an option, holding nothing if everything went fine, or an error otherwise.
    *         if the credentials are invalid, a [[GeneralErrorCodes.InvalidAppSecret]] error is returned
    *         if the group doesn't exist, a [[GeneralErrorCodes.GroupNotFound]] error is returned
    *         if the user doesn't exist or is not a member of the group, a [[GeneralErrorCodes.UserNotFound]] error is returned
    *         if you don't have the permission to remove this user, a [[GeneralErrorCodes.MissingPermission]] error is returned
    *         a [[GeneralErrorCodes.UnknownError]] might be produced if something happens
    */
  def removeUserFromGroup(group: String, userId: Int): Future[Option[ErrorCode]] = {
    ws.url(apiBase + "/api/groups/" + group + "/members/" + userId)
      .asCurrentApp
      .delete()
      .map(mapResponse("group_add_user", _ => None, e => Some(e)))
  }
}

object AuthApi {
  @Inject()
  def apply(client: WSClient)(implicit executionContext: ExecutionContext, config: Configuration): AuthApi = {
    val clientSecret: String = config.get[String]("jiauth.clientSecret")
    val apiRoot: String = config.get[String]("jiauth.baseUrl")

    new AuthApi(client, apiRoot, clientSecret)
  }
}