package models

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
  def createApiKey(app: ApiKey): Future[Int] = Future(db.withConnection(implicit c => SqlUtils.insertOne("apps", app)))

  def createApiKey(name: String, user: Int): Future[Int] =
    createApiKey(ApiKey(None, user, clientSecret = RandomUtils.randomString(32), name))


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

  /**
   * Get all the apps owned by a specific user
   *
   * @param owner the owner of the apps
   * @return all the apps owned by the given user
   */
  def getApiKeysByOwner(owner: Int): Future[Seq[ApiKey]] = Future(db.withConnection(implicit c =>
    SQL"SELECT * FROM api_keys WHERE app_created_by = $owner".as(AppRowParser.*)))
}
