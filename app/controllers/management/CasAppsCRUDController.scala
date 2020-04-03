package controllers.management

import data.UserSession
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
class CasAppsCRUDController @Inject()(cc: MessagesControllerComponents,
                                      tickets: TicketsModel,
                                      apps: ServicesModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) {
  private val createUpdateForm = Form(
    mapping(
      "name" -> nonEmptyText(5, 100),
      "redirect_url" -> optional(text).transform[Option[String]](opt => opt.map(_.trim).filter(_.nonEmpty), opt => opt)
    )(Tuple2.apply)(Tuple2.unapply)
  )

  private val groupForm = Form(
    mapping(
      "group" -> nonEmptyText(5, 100)
    )(p => p)(p => Some(p))
  )

  private val domainForm = Form(
    mapping(
      "domain" -> nonEmptyText(5, 100)
    )(p => p)(p => Some(p))
  )

  private val allowedForm = Form(
    mapping(
      "allowedApp" -> number
    )(p => p)(p => Some(p))
  )

  def createAppForm: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageCasApps) { session =>
      Future(Ok(views.html.management.cas.createUpdate(session, createUpdateForm)))
    }
  }

  def createApp: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageCasApps) { implicit session =>
      createUpdateForm.bindFromRequest().fold(withErrors => {
        BadRequest(views.html.management.cas.createUpdate(session, withErrors))
      }, {
        case (name, domain) =>
        apps.createApp(name, domain).map(id => Redirect(routes.CasAppsCRUDController.getApp(id)))})
    }
  }

  def updateApp(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageCasApps) { implicit session =>
      apps.getCasServiceById(id).flatMap {
        case Some(app) =>
          createUpdateForm.bindFromRequest().fold(
            withErrors => {
              BadRequest(views.html.management.cas.createUpdate(session, withErrors, Some(id)))
            }, { case (name, redirect) =>
              apps
                .updateApp(app.copy(serviceName = name, serviceRedirectUrl = redirect))
                .map(_ => Redirect(routes.CasAppsCRUDController.getApp(id)))
            })
        case None => NotFound(ManagementTools.error("Application introuvable", "L'application recherchée n'existe pas, ou ne vous est pas accessible."))
      }
    }
  }

  def updateAppForm(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageCasApps) { implicit session =>
      apps.getCasServiceById(id).flatMap {
        case Some(app) =>
          Ok(views.html.management.cas.createUpdate(session, createUpdateForm.fill((app.serviceName, app.serviceRedirectUrl)), Some(id)))
        case None => NotFound(ManagementTools.error("Application introuvable", "L'application recherchée n'existe pas, ou ne vous est pas accessible."))
      }
    }
  }

  def getApp(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageCasApps) { implicit session =>
      apps.getServiceById(id).flatMap {
        case Some(apps.ServiceData(service, requiredGroups, allowedGroups, domains, accessFrom)) =>
          Ok(views.html.management.cas.view(session, service, domains, requiredGroups, allowedGroups, accessFrom))
        case None => NotFound(ManagementTools.error("Application introuvable", "L'application recherchée n'existe pas, ou ne vous est pas accessible."))
      }
    }
  }

  def whitelistDomainPost(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageCasApps) { implicit session =>
      domainForm.bindFromRequest().fold(withErrors => {
        Redirect(controllers.management.routes.CasAppsCRUDController.getApp(id))
      }, name => {
        apps.addDomain(id, name)
          .map { _ => Redirect(controllers.management.routes.CasAppsCRUDController.getApp(id)) }
      })
    }
  }

  def whitelistDomainDelete(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageCasApps) { implicit session =>
      domainForm.bindFromRequest().fold(withErrors => {
        Redirect(controllers.management.routes.CasAppsCRUDController.getApp(id))
      }, name => {
        apps.removeDomain(id, name)
          .map { _ => Redirect(controllers.management.routes.CasAppsCRUDController.getApp(id)) }
      })
    }
  }

  def groupsAddPost(id: Int, required: Boolean): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageCasApps) { implicit session =>
      groupForm.bindFromRequest().fold(withErrors => {
        Redirect(controllers.management.routes.CasAppsCRUDController.getApp(id))
      }, name => {
        apps.addGroup(id, name, required)
          .map { _ => Redirect(controllers.management.routes.CasAppsCRUDController.getApp(id)) }
      })
    }
  }

  def groupsDeletePost(id: Int, required: Boolean): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageCasApps) { implicit session =>
      groupForm.bindFromRequest().fold(withErrors => {
        Redirect(controllers.management.routes.CasAppsCRUDController.getApp(id))
      }, name => {
        apps.removeGroup(id, name, required)
          .map { _ => Redirect(controllers.management.routes.CasAppsCRUDController.getApp(id)) }
      })
    }
  }

  def allowedAppsAdd(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageCasApps) { implicit session =>
      allowedForm.bindFromRequest().fold(withErrors => {
        Redirect(controllers.management.routes.CasAppsCRUDController.getApp(id))
      }, allowed => {
        apps.addAllowedService(id, allowed)
          .map { _ => Redirect(controllers.management.routes.CasAppsCRUDController.getApp(id)) }
      })
    }
  }

  def allowedAppsDelete(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageCasApps) { implicit session =>
      allowedForm.bindFromRequest().fold(withErrors => {
        Redirect(controllers.management.routes.CasAppsCRUDController.getApp(id))
      }, allowed => {
        apps.removeAllowedService(id, allowed)
          .map { _ => Redirect(controllers.management.routes.CasAppsCRUDController.getApp(id)) }
      })
    }
  }
}
