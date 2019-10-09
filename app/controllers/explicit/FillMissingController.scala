package controllers.explicit

import data._
import javax.inject.Inject
import models.{AppsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import play.twirl.api.HtmlFormat
import services.{HashService, ReCaptchaService}
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class FillMissingController @Inject()(cc: MessagesControllerComponents,
                                      hashes: HashService,
                                      captcha: ReCaptchaService)(implicit ec: ExecutionContext, apps: AppsModel, tickets: TicketsModel,                                       users: UsersModel,
                                                                 mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) with I18nSupport {

  private val updateForm = Form(
    mapping(
      "address" -> nonEmptyText(2, 200),
      "addressComplement" -> optional(nonEmptyText(2, 200)),
      "postCode" -> nonEmptyText(3, 10),
      "city" -> nonEmptyText(3, 100),
      "country" -> nonEmptyText(2, 100)
    )(Tuple5.apply)(Tuple5.unapply))

  private def displayForm(form: Form[_], app: Option[String])(implicit rq: RequestHeader): Future[HtmlFormat.Appendable] =
    views.html.fillMissing.fillMissing(form, app)

  def fillMissingGet(app: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    ExplicitTools.ifLoggedIn(app) { user =>
      displayForm(updateForm, app).map(f => Ok(f))
    }
  }

  def fillMissingPost(app: Option[String]): Action[AnyContent] = Action.async { implicit rq =>
    ExplicitTools.ifLoggedIn(app) { user =>
      updateForm.bindFromRequest().fold(withErrors => {
        displayForm(withErrors, app).map(f => BadRequest(f))
      }, data => {
        val (address, addressComplement, postCode, city, country) = data

        val addr = Address(user.id, address, addressComplement, postCode, city, country)
        // Password is hashed by register method, don't worry

        users.setAddress(addr).flatMap(succ =>

          if (succ) ExplicitTools.produceRedirectUrl(app, user.id).map(url => Redirect(url))
          else displayForm(updateForm.withGlobalError("Erreur inconnue de base de données"), app).map(f => BadRequest(f))
        )
      })
    }
  }


}
