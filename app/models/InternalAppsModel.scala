package models

import anorm.SqlParser._
import anorm._
import javax.inject.Inject
import utils.CAS

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author zyuiop
 */
class InternalAppsModel @Inject()(dbApi: play.api.db.DBApi)(implicit ec: ExecutionContext) {
  private val db = dbApi database "default"

  def isInternalApp(url: String): Future[Boolean] = {
    CAS.getServiceDomain(url) match {
      case Some(domain) => Future(db.withConnection {
        implicit c =>
          SQL"SELECT COUNT(*) FROM internal_domains WHERE domain_name = $domain"
            .as(scalar[Int].single) > 0
      })
      case None => Future.successful(false)
    }
  }

  def isInternalAppSafe(url: String): Future[Option[Boolean]] = {
    CAS.getServiceDomain(url) match {
      case Some(domain) => Future(db.withConnection {
        implicit c =>
          SQL"SELECT safe_redirection FROM internal_domains WHERE domain_name = $domain"
            .as(bool("safe_redirection").singleOpt)
      })
      case None => Future.successful(None)
    }
  }

  def getInternalApps = Future(db.withConnection {
    implicit c =>
      SQL"SELECT * FROM internal_domains"
        .as(str("domain_name").*)
  })

  def createInternalApp(url: String): Future[Boolean] = {
    CAS.getServiceDomain(url) match {
      case Some(domain) => Future(db.withConnection {
        implicit c =>
          SQL"INSERT INTO internal_domains(domain_name) VALUES ($domain)"
            .execute()

          true
      })
      case None => Future.successful(false)
    }
  }

  def deleteInternalApp(url: String): Future[Boolean] = {
    CAS.getServiceDomain(url) match {
      case Some(domain) => Future(db.withConnection {
        implicit c =>
          SQL"DELETE FROM internal_domains WHERE domain_name = $domain"
            .execute()
      })
      case None => Future.successful(false)
    }
  }
}
