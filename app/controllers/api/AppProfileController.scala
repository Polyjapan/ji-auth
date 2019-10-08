package controllers.api

import ch.japanimpact.auth.api.UserProfile
import ch.japanimpact.auth.api._
import ch.japanimpact.auth.api.constants.GeneralErrorCodes._
import javax.inject.Inject
import models.{AppsModel, GroupsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import utils.Implicits._

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class AppProfileController @Inject()(cc: ControllerComponents,
                                     tickets: TicketsModel,
                                     users: UsersModel,
                                     groups: GroupsModel)(implicit ec: ExecutionContext, apps: AppsModel, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {


  def getUserProfile(user: Int): Action[AnyContent] = Action.async { implicit rq =>
    ApiUtils.withApp { _ =>
      users.getUserProfile(id = user).map {
        case Some((user, address)) =>
          val result = UserProfile(user.id.get, user.email, user.firstName.get, user.lastName.get, user.phoneNumber, address.toUserAddress)
          Ok(Json.toJson(result))
        case None => !UserNotFound
      }
    }
  }
}
