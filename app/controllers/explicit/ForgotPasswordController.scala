package controllers.explicit

import javax.inject.Inject
import models.{AppsModel, TicketsModel}
import play.api.Configuration
import play.api.libs.mailer.MailerClient
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.Implicits._

import scala.concurrent.ExecutionContext

/**
  * @author Louis Vialar
  */
class ForgotPasswordController @Inject()(cc: ControllerComponents,
                                         tickets: TicketsModel,
                                         apps: AppsModel)(implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration) extends AbstractController(cc) {



  def forgotPasswordGet(app: Option[String]): Action[AnyContent] = Action { implicit rq =>
    Ok
  }

  def forgotPasswordPost(app: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    Ok
  }


}
