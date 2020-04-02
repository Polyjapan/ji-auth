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
}
