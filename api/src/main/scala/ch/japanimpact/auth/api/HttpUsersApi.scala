package ch.japanimpact.auth.api

import akka.Done
import ch.japanimpact.auth.api.apitokens.APITokensService
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.cache.AsyncCacheApi
import play.api.libs.json.Json
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}


/**
 * @author Louis Vialar
 */
@Singleton
class HttpUsersApi @Inject()(ws: WSClient, config: Configuration, tokens: APITokensService, cache: AsyncCacheApi)(implicit ec: ExecutionContext)
  extends AbstractHttpApi(Set("users/*"), ws, config, tokens, cache) with UsersApi {

  override def getUsers: Result[Iterable[UserData]] = {
    cacheOnlySuccess("users") {
      withToken("/users")(_.get())(_.as[Seq[UserData]]).map(_.map {
        value => value.map(user => user.id.get -> user).toMap
      })
    }.map(_.map(_.values))
  }

  override def searchUsers(query: String): Result[Seq[UserProfile]] = {
    cacheOnlySuccess(s"users.search.$query") {
      withToken("/users")(_.withQueryStringParameters("q" -> query).get())(_.as[Seq[UserProfile]])
    }
  }

  override def getUsersWithIds(ids: Set[Int]): Result[PartialFunction[Int, UserData]] = {
    def doGetUserProfiles(ids: Set[Int]): Result[Map[Int, UserData]] = {
      if (ids.size > 300) {
        val (left, right) = ids.splitAt(300) // Split the request

        val l = doGetUserProfiles(left)
        val r = doGetUserProfiles(right)

        Future.reduceLeft(List(l, r)) {
          case (Right(lMap), Right(rMap)) => Right(lMap ++ rMap)
          case (Left(lErr), _) => Left(lErr)
          case (_, Left(rErr)) => Left(rErr)
        }
      } else if (ids.nonEmpty) {
        withToken("/users/" + ids.mkString(","))(_.get)(_.as[Map[String, UserData]])
          .map(_.map(_.map { case (k, v) => k.toInt -> v }))
      } else {
        Future.successful(Right(Map()))
      }
    }

    cache.get[Map[Int, UserData]]("users").flatMap {
      case Some(map) =>
        val found = map.view.filterKeys(ids)
        if (found.sizeCompare(ids.size) != 0) {
          val missing = (ids removedAll found.keySet)
          doGetUserProfiles(missing).map(_.map(_ ++ found))
        } else {
          Future.successful(Right(found))
        }
      case None =>
        doGetUserProfiles(ids)
    }
  }

  override def user(userId: Int): UserApi = new HttpUserApi(userId)

  class HttpUserApi(override val userId: Int) extends UserApi {
    override def get: Result[UserData] =
      cacheOnlySuccess(s"users.$userId") {
        cache.get[Map[Int, UserData]]("users").flatMap {
          case Some(map) if map.contains(userId) => Future.successful(Right(map(userId)))
          case _ => withToken(s"/users/$userId")(_.get())(_.as[UserData])
        }
      }

    override def forceLogOut(): Result[Done] =
      withTokenToDone(s"/users/$userId/logout")(_.get())

    override def forceConfirmEmail(): Result[Done] =
      withTokenToDone(s"/users/$userId/confirmEmail")(_.get())

    override def addScope(scope: String): Result[Done] =
      withTokenToDone(s"/users/$userId/scopes")(_.post(scope))

    override def removeScope(scope: String): Result[Done] =
      withTokenToDone(s"/users/$userId/scopes/delete")(_.post(scope))

    override def update(profile: UserProfile): Result[Done] =
      withTokenToDone(s"/users/$userId")(_.put(Json.toJson(profile))).map {
        e =>
          if (e.isRight) {
            cache.remove(s"users.$userId")
            cache.remove("users")
          }
          e
      }
  }

}


