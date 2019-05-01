package models

import com.google.common.base.Preconditions
import data._
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author zyuiop
  */
class AppsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  /**
    * Get an app registered in the system by its public clientId
    * @param clientId the clientId of the app
    * @return the app
    */
  def getApp(clientId: String): Future[Option[App]] =
    db.run(apps.filter(row => row.clientId === clientId).result.headOption)

  /**
    * Get an app registered in the system by its public clientId and private clientSecret
    * @param clientId the clientId to look for
    * @param clientSecret the clkientSecret to look for
    * @return an optional app with the same clientId and clientSecret as requested
    */
  def getAuthentifiedApp(clientId: String, clientSecret: String): Future[Option[App]] =
    db.run(apps.filter(row => row.clientId === clientId && row.clientSecret === clientSecret).result.headOption)

  /**
    * Get the name of an app, given a clientId
    * @param clientId an optional clientId. If empty, the app name will be empty too.
    * @return the name of the app, or None if this app doesn't exist
    */
  def getAppName(clientId: Option[String]): Future[Option[String]] = {
    clientId match {
      case Some(clientId) => getApp(clientId).map(opt => opt.map(_.name))
      case None => Future(None)
    }
  }

  /**
    * Create an app
    *
    * @param app the app to create
    * @return a future hodling the id of the inserted app
    */
  def createApp(app: App): Future[Int] =
    db.run((apps returning apps.map(_.id)) += app)

  /**
    * Updates an app whose id is set
    *
    * @param app the app to update/insert
    * @return the number of updated rows in a future
    */
  def updateApp(app: App): Future[Int] = {
    Preconditions.checkArgument(app.id.isDefined)
    db.run(apps.filter(_.id === app.id.get).update(app))
  }
}
