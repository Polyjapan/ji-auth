package models

import anorm.SqlParser._
import anorm._
import com.google.common.base.Preconditions
import data._
import javax.inject.Inject
import utils.{CAS, RandomUtils}

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author zyuiop
 */
class AppsModel @Inject()(dbApi: play.api.db.DBApi)(implicit ec: ExecutionContext) {
  private val db = dbApi database "default"

  def getCasApp(url: String): Future[Option[CasService]] = {
    CAS.getServiceDomain(url) match {
      case Some(domain) => Future(db.withConnection {
        implicit c =>
          SQL"SELECT cs.service_id, cs.service_name FROM cas_domains JOIN cas_services cs on cas_domains.service_id = cs.service_id WHERE domain = $domain"
            .as((int("service_id") ~ str("service_name")).map { case id ~ name => CasService(id, name) }.singleOpt)
      })
      case None => Future.successful(None)
    }
  }

  /**
   * Get an app registered in the system by its public clientId
   *
   * @param clientId the clientId of the app
   * @return the app
   */
  def getApp(clientId: String): Future[Option[App]] = Future(db.withConnection { implicit conn =>
    SQL"SELECT * FROM apps WHERE client_id = $clientId"
      .as(AppRowParser.singleOpt)
  })

  /**
   * Get an app registered in the system by its internal id and its owner
   *
   * @param appId   the id of the app
   * @param ownerId the id of the owner of the app
   * @return the app, if found
   */
  def getAppByIdAndOwner(appId: Int, ownerId: Int): Future[Option[App]] = Future(db.withConnection { implicit c =>
    SQL"SELECT * FROM apps WHERE app_id = $appId AND app_created_by = $ownerId"
      .as(AppRowParser.singleOpt)
  })


  /**
   * Get an app registered in the system by its public clientId and private clientSecret
   *
   * @param clientId     the clientId to look for
   * @param clientSecret the clkientSecret to look for
   * @return an optional app with the same clientId and clientSecret as requested
   */
  def getAuthentifiedApp(clientId: String, clientSecret: String): Future[Option[App]] = Future(db.withConnection { implicit c =>
    SQL"SELECT * FROM apps WHERE client_id = $clientId AND client_secret = $clientSecret"
      .as(AppRowParser.singleOpt)
  })

  /**
   * Get the name of an app, given a clientId
   *
   * @param clientId an optional clientId. If empty, the app name will be empty too.
   * @return the name of the app, or None if this app doesn't exist
   */
  def getAppName(clientId: Option[String]): Future[Option[String]] = {
    clientId match {
      case Some(clientId) => getApp(clientId).map(opt => opt.map(_.appName))
      case None => Future(None)
    }
  }

  /**
   * Create an app
   *
   * @param app the app to create
   * @return a future hodling the id of the inserted app
   */
  def createApp(app: App): Future[Int] = Future(db.withConnection(implicit c => SqlUtils.insertOne("apps", app)))

  def createApp(name: String, redirectUrl: String, emailCallbackUrl: String, captcha: Option[String], user: Int): Future[Int] =
    createApp(App(None, user, RandomUtils.randomString(32), RandomUtils.randomString(32), name, redirectUrl, emailCallbackUrl, captcha))

  /**
   * Updates an app whose id is set
   *
   * @param app the app to update/insert
   * @return the number of updated rows in a future
   */
  def updateApp(app: App): Future[Int] = {
    Preconditions.checkArgument(app.appId.isDefined)

    Future(db.withConnection(implicit c => SqlUtils.replaceOne("apps", app, "appId")))
  }

  /**
   * Get all the apps owned by a specific user
   *
   * @param owner the owner of the apps
   * @return all the apps owned by the given user
   */
  def getAppsByOwner(owner: Int): Future[Seq[App]] = Future(db.withConnection(implicit c =>
    SQL"SELECT * FROM apps WHERE app_created_by = $owner".as(AppRowParser.*)))
}
