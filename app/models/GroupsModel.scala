package models

import java.sql.SQLException

import anorm.SqlParser._
import anorm._
import data.{Group, GroupMember, GroupRowParser, RegisteredUser, GroupMemberRowParser, RegisteredUserRowParser}
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author zyuiop
 */
class GroupsModel @Inject()(dbApi: play.api.db.DBApi)(implicit ec: ExecutionContext) {
  private val db = dbApi database "default"


  def getGroups(app: data.ApiKey, userId: Int): Future[Set[String]] = Future(db.withConnection { implicit c =>
    SQL"""SELECT name FROM `groups`
      JOIN groups_members gm1 on `groups`.id = gm1.group_id and gm1.can_read_members = TRUE and gm1.user_id = ${app.appCreatedBy}
      JOIN groups_members gm2 on `groups`.id = gm2.group_id and gm2.user_id = ${userId}
    """.as(str(1).*).toSet
  })


  def getGroupsByMember(member: Int): Future[Seq[Group]] = Future(db.withConnection { implicit c =>
    SQL"SELECT * FROM `groups` JOIN groups_members gm on `groups`.id = gm.group_id WHERE gm.user_id = $member"
      .as(GroupRowParser.*)
  })

  /**
   * Create a group, and return true if the group could be created correctly
   */
  def createGroup(name: String, displayName: String, user: Int): Future[Boolean] = Future(db.withConnection { implicit c =>
    try {
      val groupId = SqlUtils.insertOne("groups", Group(None, user, name, displayName))
      SqlUtils.insertOne("groups_members", GroupMember(groupId, user, true, true, true))

      true
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        false
    }
  })

  /**
   * Update a group, and return true if the group could be updated correctly
   */
  def updateGroup(oldName: String, name: String, displayName: String): Future[Boolean] = {
    if (oldName == name) {
      Future(db.withConnection(implicit c => {
        SQL"UPDATE `groups` SET display_name = $displayName WHERE name = $name".executeUpdate() > 0
      }))
    } else {
      Future(db.withConnection(implicit c => {
        SQL"UPDATE `groups` SET name = $name, display_name = $displayName WHERE name = $oldName".executeUpdate() > 0
      }))
    }


  }

  def getGroup(name: String): Future[Option[Group]] = Future(db.withConnection{ implicit c =>
    SQL"SELECT * FROM `groups` WHERE name = $name".as(GroupRowParser.singleOpt)
  })

  def getGroupMembers(id: Int): Future[Seq[(GroupMember, RegisteredUser)]] =
    Future(db.withConnection{ implicit c =>
      SQL"SELECT * FROM groups_members JOIN users u on groups_members.user_id = u.id WHERE group_id = $id"
        .as((GroupMemberRowParser ~ RegisteredUserRowParser).map { case gm ~ u => (gm, u)}.*)
    })

  def getGroupMembership(name: String, user: Int): Future[Option[GroupMember]] =
    Future(db.withConnection{ implicit c =>
      SQL"SELECT gm.* FROM `groups` JOIN groups_members gm on `groups`.id = gm.group_id WHERE name = $name AND user_id = $user"
        .as(GroupMemberRowParser.singleOpt)
    })

  def getGroupIfMember(name: String, user: Int): Future[Option[(Group, GroupMember, RegisteredUser)]] =
    Future(db.withConnection{ implicit c =>
      SQL"SELECT * FROM `groups` JOIN groups_members gm on `groups`.id = gm.group_id JOIN users u on `groups`.owner = u.id WHERE name = $name AND user_id = $user"
        .as((GroupRowParser ~ GroupMemberRowParser ~ RegisteredUserRowParser).map { case a ~ b ~ c => (a, b, c) }.singleOpt)
    })

  def addMember(groupId: Int, userId: Int): Future[Boolean] = {
    Future(db.withConnection(implicit c => {
      try {
        SqlUtils.insertOne("groups_members", GroupMember(groupId, userId, false, false, false)) > 0
      } catch {
        case e: SQLException =>
          e.printStackTrace()
          false
      }
    }))
  }

  def removeMember(groupId: Int, userId: Int): Future[Boolean] = {

    Future(db.withConnection {
      implicit c => SQL"DELETE FROM groups_members WHERE group_id = $groupId AND user_id = $userId".execute()
    })
  }

}
