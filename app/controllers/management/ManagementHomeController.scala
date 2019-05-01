package controllers.management

import javax.inject.Inject
import models.{AppsModel, GroupsModel, TicketsModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class ManagementHomeController @Inject()(cc: ControllerComponents,
                                         tickets: TicketsModel,
                                         apps: AppsModel,
                                         groups: GroupsModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {


  def home: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { session =>
      apps.getAppsByOwner(session.id).flatMap(apps => {
        groups.getGroupsByMember(session.id).map(groups => {
          Ok(views.html.management.home(session, groups.toSet, apps.toSet))
        })
      })

    }
  }
}
