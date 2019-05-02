package controllers.api

import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import data.GroupMember
import javax.inject.Inject
import models.{AppsModel, GroupsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import utils.Implicits._

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class AppGroupsController @Inject()(cc: ControllerComponents,
                                    tickets: TicketsModel,
                                    users: UsersModel,
                                    groups: GroupsModel)(implicit ec: ExecutionContext, apps: AppsModel, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {


  def addMemberToGroup(groupName: String): Action[JsValue] = Action.async(parse.json) { implicit rq =>
    if (rq.hasBody) {
      val body = rq.body
      val interesting = (body \ "clientId").validate[Int]

      if (interesting.isSuccess) {
        val userId = interesting.get

        ApiUtils.withApp { app =>
          groups.getGroupMembership(groupName, app.createdBy).flatMap {
            case Some(GroupMember(groupId, _, canManage, _, _)) if canManage =>
              users.getUserById(userId).map {
                case Some(_) =>
                  // User exists
                  groups.addMember(groupId, userId)
                  Ok
                case None => !UserNotFound
              }
            case Some(_) =>
              !MissingPermission
            case _ =>
              !GroupNotFound
          }
        }
      } else !MissingData
    }
    else !MissingData // No body or body parse fail ==> invalid input
  }

  def removeMemberFromGroup(groupName: String, member: Int): Action[AnyContent] = Action.async { implicit rq =>
    ApiUtils.withApp { app =>
      groups.getGroupIfMember(groupName, app.createdBy).flatMap {
        case Some((_, GroupMember(groupId, _, canManage, _, admin), owner)) if canManage && member != owner.id.get =>
          groups.getGroupMembership(groupName, member).map {
            case Some(GroupMember(_, _, _, _, memberAdmin)) =>
              if (memberAdmin && !admin) {
                !MissingPermission
              } else {
                groups.removeMember(groupId, member)
                Ok
              }

            case None => !UserNotFound
          }
        case Some(_) =>
          !MissingPermission
        case _ =>
          !GroupNotFound
      }

    }

  }
}
