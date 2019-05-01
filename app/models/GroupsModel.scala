package models

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
}
