package controllers.management

import data.{GroupMember, RegisteredUser, UserSession}
import javax.inject.Inject
import models.{AppsModel, GroupsModel, TicketsModel}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.data.validation.Constraints
import play.api.libs.mailer.MailerClient
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents, MessagesRequestHeader, RequestHeader, Result}
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class GroupCRUDController @Inject()(cc: MessagesControllerComponents,
                                    tickets: TicketsModel,
                                    groups: GroupsModel,
                                    apps: AppsModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) {

  private val nameConstraint = Constraints.pattern("^[a-zA-Z0-9_-]+$".r,
    error = "L'identifiant ne peut contenir que des caractères alphanumériques ainsi que des _ et -")

  private val form = Form(mapping("name" -> nonEmptyText(5, 100).verifying(nameConstraint),
    "displayName" -> nonEmptyText(5, 100))(Tuple2.apply)(Tuple2.unapply))

  def createGroupForm: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.CreateGroup) { session =>
      Future(Ok(views.html.management.groups.createUpdate(session, form)))
    }
  }

  def createGroup: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.CreateGroup) { implicit session =>
      form.bindFromRequest().fold(withErrors => {
        BadRequest(views.html.management.groups.createUpdate(session, withErrors))
      }, data => {
        val (name, displayName) = data

        groups.createGroup(name, displayName, session.id).map(createOrUpdateResult(data))
      })
    }
  }


  def updateGroup(name: String): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { implicit session =>
      groups.getGroupMembership(name, session.id).flatMap {
        case Some(membership) =>
          if (membership.isAdmin) {
            form.bindFromRequest().fold(withErrors => {
              BadRequest(views.html.management.groups.createUpdate(session, withErrors, Some(name)))
            }, data => {
              val (newName, displayName) = data

              groups.updateGroup(name, newName, displayName).map(createOrUpdateResult(data))
            })
          } else {
            Forbidden(ManagementTools.error("Permissions insuffisantes", "Vous n'avez pas le droit de modifier ce groupe."))
          }
        case None => NotFound(ManagementTools.error("Groupe introuvable", "Le groupe recherché n'existe pas, ou ne vous est pas accessible."))
      }
    }
  }

  private def createOrUpdateResult(data: (String, String))(implicit session: UserSession, rq: MessagesRequestHeader): Boolean => Result = { success =>
    if (success) {
      Redirect(controllers.management.routes.GroupCRUDController.getGroup(data._1))
    } else {
      BadRequest(views.html.management.groups.createUpdate(session,
        form.fill(data).withError("name", "Le nom est déjà utilisé.")
      ))
    }
  }

  def updateGroupForm(name: String): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { implicit session =>
      groups.getGroupIfMember(name, session.id).flatMap {
        case Some((group, membership, owner)) =>
          if (membership.isAdmin) {
            Ok(views.html.management.groups.createUpdate(session, form.fill((name, group.displayName)), Some(name)))
          } else {
            Forbidden(ManagementTools.error("Permissions insuffisantes", "Vous n'avez pas le droit de modifier ce groupe."))
          }
        case None => NotFound(ManagementTools.error("Groupe introuvable", "Le groupe recherché n'existe pas, ou ne vous est pas accessible."))
      }
    }
  }

  def getGroup(name: String): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { implicit session =>
      groups.getGroupIfMember(name, session.id).flatMap {
        case Some((group, membership, owner)) =>
          val members: Future[Seq[(GroupMember, RegisteredUser)]] =
            if (membership.canReadMembers) {
              groups.getGroupMembers(group.id.get)
            } else Future(Seq())

          members.map(seq => Ok(views.html.management.groups.view(session, group, owner, membership, seq)))

        case None => NotFound(ManagementTools.error("Groupe introuvable", "Le groupe recherché n'existe pas, ou ne vous est pas accessible."))
      }
    }
  }

  def deleteGroup(name: String) = Action.async { implicit rq =>
    ???
  }
}
