package controllers.management

import data.{GroupMember, RegisteredUser, UserSession}
import javax.inject.Inject
import models.{AppsModel, TicketsModel}
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
class AppsCRUDController @Inject()(cc: MessagesControllerComponents,
                                   tickets: TicketsModel,
                                   apps: AppsModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) {
  private val form = Form(
    mapping(
      "name" -> nonEmptyText(5, 100),
      "redirect_url" -> text,
      "email_callback_url" -> text,
      "recaptcha_private_key" -> optional(nonEmptyText)
    )(Tuple4.apply)(Tuple4.unapply)
  )

  def createAppForm: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.CreateApp) { session =>
      Future(Ok(views.html.management.apps.createUpdate(session, form)))
    }
  }

  def createApp: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifPermission(UserSession.CreateApp) { implicit session =>
      form.bindFromRequest().fold(withErrors => {
        BadRequest(views.html.management.apps.createUpdate(session, withErrors))
      }, data => {
        val (name, redirect, email, captcha) = data

        apps.createApp(name, redirect, email, captcha, session.id).map(
          id => Redirect(routes.AppsCRUDController.getApp(id)))
      })
    }
  }

  def updateApp(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { implicit session =>
      apps.getAppByIdAndOwner(id, session.id).flatMap {
        case Some(app) =>
          form.bindFromRequest().fold(
            withErrors => {
              BadRequest(views.html.management.apps.createUpdate(session, withErrors, Some(id)))
            }, data => {
            val (name, redirect, email, captcha) = data

            apps
              .updateApp(app.copy(appName = name, redirectUrl = redirect, emailCallbackUrl = email, recaptchaPrivateKey = captcha))
              .map(
                _ => Redirect(routes.AppsCRUDController.getApp(id)))
          })
        case None => NotFound(ManagementTools.error("Application introuvable", "L'application recherchée n'existe pas, ou ne vous est pas accessible."))
      }
    }
  }

  def updateAppForm(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { implicit session =>
      apps.getAppByIdAndOwner(id, session.id).flatMap {
        case Some(app) =>
          Ok(views.html.management.apps.createUpdate(session, form.fill((app.appName, app.redirectUrl, app.emailCallbackUrl, app.recaptchaPrivateKey)), Some(id)))
        case None => NotFound(ManagementTools.error("Application introuvable", "L'application recherchée n'existe pas, ou ne vous est pas accessible."))
      }
    }
  }

  def getApp(id: Int): Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { implicit session =>
      apps.getAppByIdAndOwner(id, session.id).flatMap {
        case Some(app) =>
          Ok(views.html.management.apps.view(session, app))
        case None => NotFound(ManagementTools.error("Application introuvable", "L'application recherchée n'existe pas, ou ne vous est pas accessible."))
      }
    }
  }
}
