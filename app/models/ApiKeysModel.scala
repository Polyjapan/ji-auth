package models

import anorm.SqlParser._
import anorm._
import com.google.common.base.Preconditions
import data._
import javax.inject.Inject
import utils.RandomUtils

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author zyuiop
 */
class ApiKeysModel @Inject()(dbApi: play.api.db.DBApi)(implicit ec: ExecutionContext) {
  private val db = dbApi database "default"

  /**
   * Get an app registered in the system by its internal id and its owner
   *
   * @param appId   the id of the app
   * @param ownerId the id of the owner of the app
   * @return the app, if found
   */
  def getApiKeyByIdAndOwner(appId: Int, ownerId: Int): Future[Option[ApiKey]] = Future(db.withConnection { implicit c =>
    SQL"SELECT * FROM api_keys WHERE app_id = $appId AND app_created_by = $ownerId"
      .as(AppRowParser.singleOpt)
  })


  def getAllowedScopes(apiKey: Int): Future[Set[String]] = Future(db.withConnection { implicit c =>
    SQL"SELECT scope FROM api_keys_allowed_scopes WHERE api_key_id = $apiKey"
      .as(SqlParser.str("scope").*)
      .toSet
  })

  def addAllowedScope(apiKey: Int, allowed: String) = Future(db.withConnection { implicit c =>
    SQL"INSERT IGNORE INTO api_keys_allowed_scopes(api_key_id, scope) VALUES ($apiKey, $allowed)"
      .execute()
  })

  def removeAllowedScope(apiKey: Int, allowed: String) = Future(db.withConnection { implicit c =>
    SQL"DELETE FROM api_keys_allowed_scopes WHERE api_key_id = $apiKey AND scope = $allowed"
      .execute()
  })

  /**
   * Get an app registered in the system by its private clientSecret
   *
   * @param clientSecret the clkientSecret to look for
   * @return an optional app with the same clientId and clientSecret as requested
   */
  def getApiKeyBySecret(clientSecret: String): Future[Option[ApiKey]] = Future(db.withConnection { implicit c =>
    SQL"SELECT * FROM api_keys WHERE client_secret = $clientSecret"
      .as(AppRowParser.singleOpt)
  })


  /**
   * Create an app
   *
   * @param app the app to create
   * @return a future hodling the id of the inserted app
   */
  def createApiKey(app: ApiKey): Future[Int] = Future(db.withConnection(implicit c => SqlUtils.insertOne("api_keys", app)))

  def createApiKey(name: String, user: Int): Future[ApiKey] = {
    val key = ApiKey(None, user, clientSecret = RandomUtils.randomString(32), name)
    createApiKey(key).map(id => key.copy(appId = Some(id)))
  }


  /**
   * Updates an app whose id is set
   *
   * @param app the app to update/insert
   * @return the number of updated rows in a future
   */
  def updateApiKey(app: ApiKey): Future[Int] = {
    Preconditions.checkArgument(app.appId.isDefined)

    Future(db.withConnection(implicit c => SqlUtils.replaceOne("api_keys", app, "appId")))
  }

  def deleteApiKey(appId: Int): Future[Boolean] = {
    Future(db.withConnection(implicit c => SQL"DELETE FROM api_keys WHERE app_id = $appId".execute()))
  }

  private val fullApiKeyRowParser =
    ((AppRowParser ~ RegisteredUserRowParser) ~ (str("scope").?)).*
      .map(lst =>
        lst.map { case ((key ~ user) ~ (scope)) => ((key, user), scope) }
          .groupMap(_._1)(_._2)
          .map {
            case ((key, user), scopes) =>
              ApiKeyData(key, scopes.flatten.toSet, user.toUserProfile(None))
          }.toList

      )

  /**
   * Get all the apps owned by a specific user
   *
   * @param owner the owner of the apps
   * @return all the apps owned by the given user
   */
  def getApiKeysByOwner(owner: Int): Future[Seq[ApiKeyData]] = Future(db.withConnection(implicit c =>
    SQL"SELECT * FROM api_keys JOIN users u on api_keys.app_created_by = u.id LEFT JOIN api_keys_allowed_scopes akas on api_keys.app_id = akas.api_key_id WHERE app_created_by = $owner"
      .as(fullApiKeyRowParser)
  ))

  def getApiKeysById(id: Int): Future[Option[ApiKeyData]] = Future(db.withConnection(implicit c =>
    SQL"SELECT * FROM api_keys JOIN users u on api_keys.app_created_by = u.id LEFT JOIN api_keys_allowed_scopes akas on api_keys.app_id = akas.api_key_id WHERE app_id = $id"
      .as(fullApiKeyRowParser)
      .headOption
  ))

  def getApiKeys: Future[Seq[ApiKeyData]] = Future(db.withConnection(implicit c =>
    SQL"SELECT * FROM api_keys JOIN users u on api_keys.app_created_by = u.id LEFT JOIN api_keys_allowed_scopes akas on api_keys.app_id = akas.api_key_id"
      .as(fullApiKeyRowParser)
  ))
}
