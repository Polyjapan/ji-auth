package models

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import data._
import javax.inject.Inject
import utils.CAS

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author zyuiop
 */
class ServicesModel @Inject()(dbApi: play.api.db.DBApi)(implicit ec: ExecutionContext) {

  private val db = dbApi database "default"

  def getCasService(url: String): Future[Option[CasService]] = {
    CAS.getServiceDomain(url) match {
      case Some(domain) => Future(db.withConnection {
        implicit c =>
          SQL"SELECT cs.* FROM cas_domains JOIN cas_services cs on cas_domains.service_id = cs.service_id WHERE domain = $domain"
            .as(CasServiceRowParser.singleOpt)
      })
      case None => Future.successful(None)
    }
  }

  def deleteService(id: Int) = {
    Future(db.withConnection { implicit c =>
      SQL"DELETE FROM cas_services WHERE service_id = $id".execute()
    })
  }

  def hasRequiredGroups(service: Int, userId: Int): Future[Boolean] = Future(db.withConnection { implicit c =>
    val required = SQL"SELECT group_id FROM cas_required_groups WHERE service_id = $service".as(int("group_id").*).toSet
    val allowed = SQL"SELECT group_id FROM cas_allowed_groups WHERE service_id = $service".as(int("group_id").*).toSet
    val user = SQL"SELECT group_id FROM groups_members WHERE user_id = $userId".as(int("group_id").*).toSet

    required.forall(gid => user(gid)) && (allowed.isEmpty || allowed.exists(gid => user(gid)))
  })

  /**
   * Return all the services visible to a user that are portal enabled
   * @param userId
   * @return
   */
  def getPortalServicesForUser(userId: Int): Future[Iterable[CasService]] = Future(db.withConnection { implicit c =>
    val userGroups = SQL"SELECT group_id FROM groups_members WHERE user_id = $userId".as(int("group_id").*).toSet

    val services: Map[CasService, (Set[Int], Set[Int])] = SQL("SELECT cag.group_id AS allowed, crg.group_id AS required, cas_services.* FROM cas_services LEFT JOIN cas_allowed_groups cag on cas_services.service_id = cag.service_id LEFT JOIN cas_required_groups crg on cas_services.service_id = crg.service_id WHERE service_portal_display = 1 AND service_portal_title IS NOT NULL AND service_portal_login_url IS NOT NULL ORDER BY service_id")
      .as((int(1).? ~ int(2).? ~ CasServiceRowParser).*)
      .map({ case allowed ~ required ~ group => (group, allowed, required)})
      .groupMapReduce(triple => triple._1)(triple => (triple._2.toSet[Int], triple._3.toSet[Int])) {
        case ((lA: Set[Int], lB: Set[Int]), (rA: Set[Int], rB: Set[Int])) => (lA ++ rA, lB ++ rB)
      }

    services.filter {
      case (_, (allowedGroups, requiredGroups)) =>
        (allowedGroups.isEmpty || allowedGroups.exists(userGroups)) && requiredGroups.forall(userGroups)
    }.keys
  })


  def getCasServices: Future[List[CasService]] = {
    Future(db.withConnection { implicit c =>
      SQL"SELECT cs.* FROM cas_services cs".as(CasServiceRowParser.*)
    })
  }

  def getServiceById(id: Int): Future[Option[ServiceData]] = {
    Future(db.withConnection { implicit c =>
      val service = SQL"SELECT * FROM cas_services WHERE service_id = $id".as(CasServiceRowParser.singleOpt)
      val reqGroups = SQL"SELECT g.name FROM cas_required_groups JOIN `groups` g on cas_required_groups.group_id = g.id WHERE service_id = $id".as(str("name").*).toSet
      val allowGroups = SQL"SELECT g.name FROM cas_allowed_groups JOIN `groups` g on cas_allowed_groups.group_id = g.id WHERE service_id = $id".as(str("name").*).toSet
      val domains = SQL"SELECT domain FROM cas_domains WHERE service_id = $id".as(str("domain").*).toSet
      val allowAccessFrom = SQL"SELECT cs.* FROM cas_proxy_allow JOIN cas_services cs on cas_proxy_allow.allowed_service = cs.service_id WHERE cas_proxy_allow.target_service = $id"
        .as((int("service_id") ~ str("service_name")).map { case k ~ v => (k, v) }.*)

      service.map(s => ServiceData(s, reqGroups, allowGroups, domains, allowAccessFrom))
    })
  }


  def getCasServiceById(id: Int): Future[Option[CasService]] = {
    Future(db.withConnection { implicit c =>
      SQL"SELECT * FROM cas_services WHERE service_id = $id".as(CasServiceRowParser.singleOpt)
    })
  }

  def createApp(service: CasService): Future[CasService] =
    Future(db.withConnection { implicit c =>
      val id = SqlUtils.insertOne("cas_services", service.copy(serviceId = None))

      service.copy(serviceId = Some(id))
    })

  def updateApp(app: CasService): Future[Int] =
    Future(db.withConnection(implicit c => SqlUtils.replaceOne("cas_services", app, "serviceId")))

  def getDomains(serviceId: Int): Future[Set[String]] =
    Future(db.withConnection(implicit c =>
      SQL"SELECT domain FROM cas_domains WHERE service_id = $serviceId"
        .as(str("domain").*).toSet
    ))

  def addDomain(service: Int, url: String): Future[Option[String]] = {
    CAS.getServiceDomain(url) match {
      case Some(domain) => Future(db.withConnection {
        implicit c =>
          SQL"INSERT INTO cas_domains(service_id, domain) VALUES ($service, $domain)"
            .execute()

          Some(domain)
      })
      case None => Future.successful(None)
    }
  }

  def removeDomain(service: Int, url: String): Future[Boolean] = {
    CAS.getServiceDomain(url) match {
      case Some(domain) => Future(db.withConnection {
        implicit c =>
          SQL"DELETE FROM cas_domains WHERE service_id = $service AND domain = $domain"
            .execute()
      })
      case None => Future.successful(false)
    }
  }

  def addAllowedService(service: Int, allowAccessFrom: Int): Future[Boolean] =
    Future(db.withConnection {
      implicit c =>
        SQL"INSERT INTO cas_proxy_allow(target_service, allowed_service) VALUES ($service, $allowAccessFrom)"
          .execute()
    })

  def removeAllowedService(service: Int, allowed: Int): Future[Boolean] = {
    Future(db.withConnection {
      implicit c =>
        SQL"DELETE FROM cas_proxy_allow WHERE target_service = $service AND allowed_service = $allowed"
          .execute()
    })
  }

  private def getGroup(group: String)(implicit c: Connection): Option[Int] = {
    SQL"SELECT `groups`.id FROM `groups` WHERE name = $group"
      .as(int("id").singleOpt)
  }

  def addGroup(service: Int, group: String, required: Boolean): Future[Boolean] = {
    Future(db.withConnection {
      implicit c =>
        val targetTable = if (required) "cas_required_groups" else "cas_allowed_groups"

        getGroup(group) match {
          case Some(gid) => SQL("INSERT INTO " + targetTable + "(service_id, group_id) VALUES ({sid}, {gid})")
            .on("sid" -> service, "gid" -> gid)
            .execute()

            true
          case None =>
            false
        }
    })
  }

  def removeGroup(service: Int, group: String, required: Boolean): Future[Boolean] = {
    Future(db.withConnection {
      implicit c =>
        val targetTable = if (required) "cas_required_groups" else "cas_allowed_groups"

        getGroup(group) match {
          case Some(gid) => SQL("DELETE FROM " + targetTable + " WHERE service_id = {sid} AND group_id = {gid}")
            .on("sid" -> service, "gid" -> gid)
            .execute()

            true
          case None =>
            false
        }
    })
  }
}
