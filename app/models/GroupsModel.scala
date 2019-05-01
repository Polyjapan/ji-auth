package models

import data.{Group, GroupMember, RegisteredUser}
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author zyuiop
  */
class GroupsModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  def getGroups(app: data.App, userId: Int): Future[Set[String]] =
    db.run {
      groupMembers
        // Get all the groups in which the app owner is a member with read permission
        .filter(gm => gm.userId === app.createdBy && gm.canRead === true).map(_.groupId)
        .join(groups).on((gid, group) => group.id === gid).map(_._2)
        // Filter them by the one for which the user is a member
        .join(groupMembers).on((group, gm) => gm.groupId === group.id && gm.userId === userId)
        // Keep only the name
        .map(_._1.name)
        .result
    } map (r => r.toSet)


  def getGroupsByMember(member: Int): Future[Seq[Group]] =
    db.run(
      groupMembers
        .filter(_.userId === member)
        .join(groups).on((gm, g) => gm.groupId === g.id).map(_._2)
        .result)

  /**
    * Create a group, and return true if the group could be created correctly
    */
  def createGroup(name: String, displayName: String, user: Int): Future[Boolean] = {
    db.run {
      groups
        .filter(_.name === name)
        .result
        .flatMap(seq => {
          if (seq.isEmpty) {
            val group = Group(None, user, name, displayName)
            (groups returning groups.map(_.id) += group).flatMap(id => {
              (groupMembers += GroupMember(id, user, true, true, true)) map (_ => true)
            })
          } else DBIO.successful(false)
        })
        .transactionally
    }.recover {
      case e: Exception =>
        e.printStackTrace()
        false
    }
  }

  /**
    * Update a group, and return true if the group could be updated correctly
    */
  def updateGroup(oldName: String, name: String, displayName: String): Future[Boolean] = {
    if (oldName == name) {
      db.run(groups.filter(_.name === oldName).map(_.displayName).update(displayName).map(_ == 1))
    } else {
      db.run {
        groups
          .filter(_.name === name)
          .result
          .flatMap(seq => {
            if (seq.isEmpty) {
              groups
                .filter(_.name === oldName)
                .map(g => (g.name, g.displayName))
                .update((name, displayName))
                .map(_ == 1)
            } else DBIO.successful(false)
          })
          .transactionally
      }.recover {
        case e: Exception =>
          e.printStackTrace()
          false
      }
    }


  }

  def getGroup(name: String): Future[Option[Group]] =
    db.run(groups.filter(_.name === name).result.headOption)

  def getGroupMembers(id: Int): Future[Seq[(GroupMember, RegisteredUser)]] = {
    db.run(
      groupMembers.filter(_.groupId === id)
        .join(registeredUsers).on((gm, u) => u.id === gm.userId)
        .result
    )
  }

  def getGroupMembership(name: String, user: Int): Future[Option[GroupMember]] =
    db.run(
      groups
        .filter(_.name === name)
        .join(groupMembers).on((g, gm) => gm.userId === user && gm.groupId === g.id)
        .map(_._2)
        .result.headOption)

  def getGroupIfMember(name: String, user: Int): Future[Option[(Group, GroupMember, RegisteredUser)]] =
    db.run(
      groups
        .filter(_.name === name)
        .join(groupMembers).on((g, gm) => gm.userId === user && gm.groupId === g.id)
        .join(registeredUsers).on((p, u) => u.id === p._1.ownerId).map(triple => (triple._1._1, triple._1._2, triple._2))
        .result.headOption)
}
