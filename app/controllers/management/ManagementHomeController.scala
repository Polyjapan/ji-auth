package controllers.management

import javax.inject.Inject
import models.{ApiKeysModel, GroupsModel, InternalAppsModel, ServicesModel, TicketsModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

/**
 * @author Louis Vialar
 */
class ManagementHomeController @Inject()(cc: ControllerComponents,
                                         tickets: TicketsModel,
                                         apiKeys: ApiKeysModel, internal: InternalAppsModel, cas: ServicesModel,
                                         groups: GroupsModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {


  def home: Action[AnyContent] = Action.async { implicit rq =>
    ManagementTools.ifLoggedIn { session =>
      apiKeys.getApiKeysByOwner(session.id).flatMap(apps => {
        groups.getGroupsByMember(session.id).flatMap(groups => {
          internal.getInternalApps.flatMap(internalApps => {
            cas.getCasServices.map(services => {
              Ok(views.html.management.home(session, groups.toSet, apps.toSet, internalApps, services))
            })
          })
        })
      })

    }
  }
}
