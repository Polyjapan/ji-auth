package controllers.management

import data.{Group, GroupMember, RegisteredUser, UserSession}
import javax.inject.Inject
import models.{GroupsModel, ServicesModel, TicketsModel}
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
                                    apps: ServicesModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) {

  private val nameConstraint = Constraints.pattern("^[a-zA-Z0-9_-]+$".r,
    error = "L'identifiant ne peut contenir que des caractères alphanumériques ainsi que des _ et -")

  private val form = Form(mapping("name" -> nonEmptyText(5, 100).verifying(nameConstraint),
    "displayName" -> nonEmptyText(5, 100))(Tuple2.apply)(Tuple2.unapply))

  def createGroupForm: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageGroups) { session =>
      Future(Ok(views.html.management.groups.createUpdate(session, form)))
    }
  }

  def createGroup: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageGroups) { implicit session =>
      form.bindFromRequest().fold(withErrors => {
        BadRequest(views.html.management.groups.createUpdate(session, withErrors))
      }, data => {
        val (name, displayName) = data

        groups.createGroup(Group(None, name, displayName)).map(g => createOrUpdateResult(data)(session, rq)(g.isDefined))
      })
    }
  }


  def updateGroup(name: String): Action[AnyContent] = TODO

  private def createOrUpdateResult(data: (String, String))(implicit session: UserSession, rq: MessagesRequestHeader): Boolean => Result = { success =>
    if (success) {
      Redirect(controllers.management.routes.GroupCRUDController.getGroup(data._1))
    } else {
      BadRequest(views.html.management.groups.createUpdate(session,
        form.fill(data).withError("name", "Le nom est déjà utilisé.")
      ))
    }
  }

  def updateGroupForm(name: String): Action[AnyContent] = TODO
  def getGroup(name: String): Action[AnyContent] = TODO
  def deleteGroup(name: String) = TODO
}
