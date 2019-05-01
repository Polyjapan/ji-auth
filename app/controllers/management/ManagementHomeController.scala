package controllers.management

import javax.inject.Inject
import models.{AppsModel, TicketsModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class ManagementHomeController @Inject()(cc: ControllerComponents,
                                         tickets: TicketsModel,
                                         apps: AppsModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {



  def home: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { session =>
      Future(Ok(views.html.management.home(session, Set(), Set())))
    }
  }

  def forgotPasswordPost(app: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    Ok
  }


}
