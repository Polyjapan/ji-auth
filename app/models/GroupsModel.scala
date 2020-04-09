package models

import java.sql.SQLException

import anorm.SqlParser._
import anorm._
import ch.japanimpact.auth.api.UserProfile
import data.{Group, GroupData, GroupRowParser, RegisteredUser, RegisteredUserRowParser}
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author zyuiop
 */
class GroupsModel @Inject()(dbApi: play.api.db.DBApi)(implicit ec: ExecutionContext) {


  private val db = dbApi database "default"

  def getAllGroups: Future[List[GroupData]] = Future(db.withConnection { implicit c =>
    SQL"SELECT * FROM `groups` LEFT JOIN groups_allowed_scopes gas on `groups`.id = gas.group_id"
      .as((GroupRowParser ~ str("scope").?).*
        .map(lst =>
          lst.map { case a ~ b => (a, b) }
            .groupMap(_._1)(_._2)
            .map(pair => GroupData(pair._1, pair._2.flatten.toSet))
        )).toList
  })

  /**
   * Create a group, and return true if the group could be created correctly
   */
  def createGroup(group: Group): Future[Option[Group]] = Future(db.withConnection { implicit c =>
    try {
      val groupId = SqlUtils.insertOne("groups", group.copy(id = None))
      Some(group.copy(id = Some(groupId)))
    } catch {
      case e: SQLException =>
        None
    }
  })

  def updateGroup(name: String, group: Group): Future[Boolean] = Future(db.withConnection { implicit c =>
    SQL"UPDATE `groups` SET name = ${group.name}, display_name = ${group.displayName} WHERE name = $name"
      .executeUpdate() > 0
  })

  def deleteGroup(name: String) = Future(db.withConnection { implicit c =>
    SQL"DELETE FROM `groups` WHERE name = $name".execute()
  })

  def getGroup(name: String): Future[Option[GroupData]] = Future(db.withConnection {
    implicit c =>
      SQL"SELECT * FROM `groups` LEFT JOIN groups_allowed_scopes gas on `groups`.id = gas.group_id WHERE name = $name"
        .as((GroupRowParser ~ str("scope").?).*
          .map(lst =>
            lst.map { case a ~ b => (a, b) }
              .groupMap(_._1)(_._2).headOption
              .map(pair => GroupData(pair._1, pair._2.flatten.toSet))))
  })

  def addScope(group: String, scope: String) = Future(db.withConnection { implicit c =>
    SQL"INSERT IGNORE INTO groups_allowed_scopes SELECT id, $scope FROM `groups` WHERE name = $group"
      .execute()
  })

  def removeScope(group: String, scope: String) = Future(db.withConnection { implicit c =>
    val gid = SQL"SELECT id FROM groups WHERE name = $group".as(scalar[Int].singleOpt);
    gid.map { gid =>
      SQL"DELETE FROM groups_allowed_scopes WHERE group_id = $gid AND scope = $scope"
        .execute()
    }
  })

  def addMember(group: String, userId: Int): Future[Boolean] = Future(db.withConnection { implicit c =>
    SQL"INSERT IGNORE INTO groups_members(group_id, user_id) SELECT id, $userId FROM `groups` WHERE name = $group"
      .execute()
  })

  def removeMember(group: String, userId: Int): Future[Option[Boolean]] = Future(db.withConnection { implicit c =>
    val gid = SQL"SELECT id FROM groups WHERE name = $group".as(scalar[Int].singleOpt);
    gid.map { gid =>
      SQL"DELETE FROM groups_members WHERE group_id = $gid AND user_id = $userId"
        .execute()
    }
  })


  def getGroupMembers(name: String): Future[Seq[UserProfile]] =
    Future(db.withConnection {
      implicit c =>
        SQL"SELECT u.* FROM groups_members JOIN `groups` g on groups_members.group_id = g.id JOIN users u on groups_members.user_id = u.id WHERE g.name = $name"
          .as(RegisteredUserRowParser.map(_.toUserProfile(None)).*)
    })

  def getGroupsByMember(member: Int): Future[Seq[Group]] = Future(db.withConnection { implicit c =>
    SQL"SELECT * FROM `groups` JOIN groups_members gm on `groups`.id = gm.group_id WHERE gm.user_id = $member"
      .as(GroupRowParser.*)
  })

}
