package controllers.management

import data.UserSession
import javax.inject.Inject
import models.{InternalAppsModel, TicketsModel}
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
class InternalAppsCRUDController @Inject()(cc: MessagesControllerComponents,
                                           tickets: TicketsModel,
                                           apps: InternalAppsModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) {
  private val form = Form(
    mapping("domain" -> nonEmptyText(5, 100))(t => t)(t => Some(t))
  )

  def whitelistDomainGet: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageInternalApps) { session =>
      Future(Ok(views.html.management.internalapps.create(session, form)))
    }
  }

  def whitelistDomainPost: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageInternalApps) { implicit session =>
      form.bindFromRequest().fold(withErrors => {
        BadRequest(views.html.management.internalapps.create(session, withErrors))
      }, name => {

        apps.createInternalApp(name).map {
          case true => Redirect(controllers.management.routes.ManagementHomeController.home())
          case false => BadRequest(views.html.management.internalapps.create(session, form.withGlobalError("Le domaine ne ressemble pas à un nom de domaine")))
        }
      })
    }
  }

  def whitelistDomainDelete: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.ManageInternalApps) { implicit session =>
      form.bindFromRequest().fold(withErrors => {
        Redirect(controllers.management.routes.ManagementHomeController.home()) // no error handling
      }, name => {
        apps.deleteInternalApp(name).map(_ => Redirect(controllers.management.routes.ManagementHomeController.home()))
      })
    }
  }
}
