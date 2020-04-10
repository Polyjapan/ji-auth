package controllers.forms

import data._
import javax.inject.Inject
import models.UsersModel
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import play.twirl.api.HtmlFormat
import services.HashService
import utils.Implicits._
import utils.ValidationUtils

import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Louis Vialar
 */
class UpdateInfoController @Inject()(cc: MessagesControllerComponents, hashes: HashService)(implicit ec: ExecutionContext, users: UsersModel, mailer: MailerClient, config: Configuration) extends MessagesAbstractController(cc) with I18nSupport {

  private val registerForm = Form(
    mapping(
      "firstName" -> nonEmptyText(1, 50),
      "lastName" -> nonEmptyText(1, 50),
      "phone" -> ValidationUtils.validPhoneVerifier(nonEmptyText(8, 20)),

      "address" -> nonEmptyText(2, 200),
      "addressComplement" -> optional(nonEmptyText(2, 200)),
      "postCode" -> nonEmptyText(3, 10),
      "city" -> nonEmptyText(3, 100),
      "country" -> nonEmptyText(2, 100)
    )(Tuple8.apply)(Tuple8.unapply))

  private def displayForm(form: Form[_], required: Boolean = false)(implicit rq: RequestHeader): Future[HtmlFormat.Appendable] =
    views.html.updateInfo.updateInfo(form, required)

  def updateGet(required: Option[Boolean] = None): Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedIn { user: UserSession =>
      users.getUserProfile(user.id).map {
        case Some((user, address)) =>
          (user.firstName, user.lastName, user.phoneNumber.getOrElse(""),
            address.map(_.address).orNull,
            address.flatMap(_.addressComplement),
            address.map(_.postCode).orNull,
            address.map(_.city).orNull,
            address.map(_.country).orNull
          )
      }.flatMap(tuple => displayForm(registerForm.fill(tuple), required.getOrElse(false)).map(f => Ok(f)))
    }
  }

  def updatePost: Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedIn { user =>
      registerForm.bindFromRequest().fold(withErrors => {
        println(withErrors.errors)
        displayForm(withErrors).map(f => BadRequest(f))
      }, data => {
        val (firstName, lastName, phone, address, addressComplement, postCode, city, country) = data

        val addr = Address(user.id, address, addressComplement, postCode, city, country)

        users.update(user.id, firstName, lastName, phone, addr).flatMap(succ =>
          if (succ) Redirect(controllers.routes.RedirectController.redirectGet())
          else displayForm(registerForm.withGlobalError("Erreur inconnue de base de données")).map(f => InternalServerError(f))
        )
      })
    }
  }


}
