package controllers.api

import ch.japanimpact.auth.api.TicketType
import ch.japanimpact.auth.api.LoginSuccess
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import javax.inject.Inject
import models.{AppsModel, TicketsModel}
import play.api.Configuration
import play.api.mvc._
import utils.Implicits.{toOkResult, _}

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class AppLogInController @Inject()(cc: ControllerComponents,
                                   tickets: TicketsModel)(implicit ec: ExecutionContext, config: Configuration, apps: AppsModel) extends AbstractController(cc) {

  def login(clientId: String): Action[AnyContent] = Action.async { implicit rq =>
    ApiUtils.withApp { selfApp =>
      apps getApp clientId flatMap {
        case Some(otherApp) =>
          tickets.createTicketForUser(selfApp.appCreatedBy, otherApp.appId.get, TicketType.AppTicket).map(LoginSuccess.apply).map(toOkResult[LoginSuccess])
        case None =>
          !UnknownApp
      }
    }
  }
}
