package ch.japanimpact.auth.api

import akka.Done
import ch.japanimpact.auth.api.apitokens.APITokensService
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.cache.AsyncCacheApi
import play.api.libs.json.{JsNumber, Json}
import play.api.libs.ws._

import scala.concurrent.ExecutionContext


/**
 * @author Louis Vialar
 */
@Singleton
class HttpGroupsApi @Inject()(ws: WSClient, config: Configuration, tokens: APITokensService, cache: AsyncCacheApi)(implicit ec: ExecutionContext)
  extends AbstractHttpApi(Set("groups/*"), ws, config, tokens, cache) with GroupsApi {

  override def getGroups: Result[Seq[GroupData]] =
    cacheOnlySuccess("groups") {
      withToken(s"/groups")(_.get)(_.as[Seq[GroupData]])
    }

  override def createGroup(group: Group): Result[Option[Group]] =
    withToken("/groups")(_.post(Json.toJson(group)))(_.asOpt[Group])

  override def apply(groupName: String): GroupApi = new HttpGroupApi(groupName)

  class HttpGroupApi(override val name: String) extends GroupApi {
    override def get: Result[GroupData] =
      cacheOnlySuccess(s"groups.$name") {
        withToken(s"/groups/$name")(_.get)(_.as[GroupData])
      }

    override def update(group: Group): Result[Done] =
      withTokenToDone(s"/groups/$name")(_.put(Json.toJson(group)))
        .map(e => {
          if (e.isRight) cache.remove(s"groups.$name")
          e
        })

    override def delete: Result[Done] =
      withTokenToDone(s"/groups/$name")(_.delete())
        .map(e => {
          if (e.isRight) cache.remove(s"groups.$name")
          e
        })

    override def addScope(scope: String): Result[Done] =
      withTokenToDone(s"/groups/$name/scopes")(_.post(scope))
        .map(e => {
          if (e.isRight) cache.remove(s"groups.$name")
          e
        })

    override def deleteScope(scope: String): Result[Done] =
      withTokenToDone(s"/groups/$name/scopes/delete")(_.post(scope))
        .map(e => {
          if (e.isRight) cache.remove(s"groups.$name")
          e
        })

    override def addMember(user: Int): Result[Done] =
      withTokenToDone(s"/groups/$name/members")(_.post(JsNumber(user)))
        .map(e => {
          if (e.isRight) cache.remove(s"groups.$name.members")
          e
        })

    override def deleteMember(user: Int): Result[Done] =
      withTokenToDone(s"/groups/$name/members/delete")(_.post(JsNumber(user)))
        .map(e => {
          if (e.isRight) cache.remove(s"groups.$name.members")
          e
        })

    override def getMembers: Result[Seq[UserProfile]] =
      cacheOnlySuccess(s"groups.$name.members") {
        withToken(s"/groups/$name/members")(_.get)(_.as[Seq[UserProfile]])
      }
  }

}


