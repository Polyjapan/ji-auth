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
          val details = user.toUserDetails
          val result = UserProfile(user.id.get, user.email, details, address.map(_.toUserAddress))

          Ok(Json.toJson(result))
        case None => !UserNotFound
      }
    }
  }

  def searchUsers(query: String) = Action.async { implicit rq =>
    ApiUtils.withApp { _ =>
      users.searchUsers(query).map(
        seq => seq.map(user => {
          val details = user.toUserDetails
          UserProfile(user.id.get, user.email, details, None)
        })).map(r => Ok(Json.toJson(r)))
    }
  }

  def getUserProfiles(idsStr: String): Action[AnyContent] = Action.async { implicit rq =>
    val ids = idsStr.split(",").filter(_.forall(_.isDigit)).map(_.toInt).toSet

    ApiUtils.withApp { _ =>
      users.getUserProfiles(ids).map(f => f.mapValues {
        case (user, address) =>
          val details = user.toUserDetails
          val result = UserProfile(user.id.get, user.email, details, address.map(_.toUserAddress))

          result
      }).map(map => Ok(Json.toJson(map.map(p => (p._1.toString, p._2)))))
    }
  }
}
