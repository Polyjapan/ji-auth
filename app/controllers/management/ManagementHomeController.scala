package controllers.management

import javax.inject.Inject
import models.{ApiKeysModel, GroupsModel, ServicesModel, TicketsModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class ManagementHomeController @Inject()(cc: ControllerComponents,
                                         tickets: TicketsModel,
                                         apiKeys: ApiKeysModel, cas: ServicesModel,
                                         groups: GroupsModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {


  def home: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { session =>
      import data.UserSession._

      cas.getPortalServicesForUser(rq.userSession.id) map { services =>
        Ok(views.html.management.home(session, services))
      }
    }
  }
}
