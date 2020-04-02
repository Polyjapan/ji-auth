package controllers.management

import data.{GroupMember, RegisteredUser, UserSession}
import javax.inject.Inject
import models.{ApiKeysModel, ServicesModel, TicketsModel}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class ApiKeysCRUDController @Inject()(cc: MessagesControllerComponents,
                                      tickets: TicketsModel,
                                      apps: ApiKeysModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) {
  private val form = Form(
    mapping(
      "name" -> nonEmptyText(5, 100),
//      "redirect_url" -> text
    )(t => t)(t => Some(t))
  )

  def createAppForm: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageApiKeys) { session =>
      Future(Ok(views.html.management.apikeys.createUpdate(session, form)))
    }
  }

  def createApp: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageApiKeys) { implicit session =>
      form.bindFromRequest().fold(withErrors => {
        BadRequest(views.html.management.apikeys.createUpdate(session, withErrors))
      }, name => {

        apps.createApiKey(name, session.id).map(
          id => Redirect(routes.ApiKeysCRUDController.getApp(id)))
      })
    }
  }

  def updateApp(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { implicit session =>
      apps.getApiKeyByIdAndOwner(id, session.id).flatMap {
        case Some(app) =>
          form.bindFromRequest().fold(
            withErrors => {
              BadRequest(views.html.management.apikeys.createUpdate(session, withErrors, Some(id)))
            }, name => {
            apps
              .updateApiKey(app.copy(appName = name))
              .map(
                _ => Redirect(routes.ApiKeysCRUDController.getApp(id)))
          })
        case None => NotFound(ManagementTools.error("Application introuvable", "L'application recherchée n'existe pas, ou ne vous est pas accessible."))
      }
    }
  }

  def updateAppForm(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { implicit session =>
      apps.getApiKeyByIdAndOwner(id, session.id).flatMap {
        case Some(app) =>
          Ok(views.html.management.apikeys.createUpdate(session, form.fill(app.appName), Some(id)))
        case None => NotFound(ManagementTools.error("Application introuvable", "L'application recherchée n'existe pas, ou ne vous est pas accessible."))
      }
    }
  }

  def getApp(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { implicit session =>
      apps.getApiKeyByIdAndOwner(id, session.id).flatMap {
        case Some(app) =>
          Ok(views.html.management.apikeys.view(session, app))
        case None => NotFound(ManagementTools.error("Application introuvable", "L'application recherchée n'existe pas, ou ne vous est pas accessible."))
      }
    }
  }
}
