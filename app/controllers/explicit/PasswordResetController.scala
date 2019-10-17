package controllers.explicit

import java.net.URLDecoder
import java.util.Date

import data.RegisteredUser
import javax.inject.Inject
import models.{AppsModel, TicketsModel, UsersModel}
import play.api.Configuration
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.api.i18n.I18nSupport
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import services.{HashService, ReCaptchaService}
import utils.Implicits._

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Louis Vialar
  */
class PasswordResetController @Inject()(cc: MessagesControllerComponents, captcha: ReCaptchaService, hashes: HashService)
                                       (implicit ec: ExecutionContext, mailer: MailerClient, config: Configuration,
                                        tickets: TicketsModel,
                                        apps: AppsModel, users: UsersModel) extends MessagesAbstractController(cc) with I18nSupport {

  private val resetStart = Form(
    mapping(
      "email" -> email,
      "g-recaptcha-response" -> text)(Tuple2.apply)(Tuple2.unapply))


  def passwordResetGet(app: Option[String]): Action[AnyContent] = Action.async { implicit rq: Request[_] =>
    ExplicitTools.ifLoggedOut(app) {
      Ok(views.html.passwordreset.forgotPassword(resetStart, captcha.AuthSiteKey, app))
    }
  }

  def passwordResetPost(app: Option[String]): Action[AnyContent] = Action.async { implicit rq: Request[_] =>
    ExplicitTools.ifLoggedOut(app) {
      resetStart.bindFromRequest().fold(withErrors => {
        BadRequest(views.html.passwordreset.forgotPassword(withErrors, captcha.AuthSiteKey, app))
      }, data => {
        val (email, captchaResponse) = data

        // Check that the captcha is correct
        captcha.doCheckCaptchaWithExpiration(Some(captcha.AuthSecretKey), captchaResponse).flatMap(captchaResponse => {
          if (!captchaResponse.success) {
            println(captchaResponse)
            BadRequest(views.html.passwordreset.forgotPassword(resetStart.withGlobalError("Captcha incorrect"), config.get[String]("recaptcha.siteKey"), app))
          }
          else {
            users
              .resetPassword(email, (email, code) =>
                controllers.explicit.routes.PasswordResetController.passwordResetChangeGet(email, code, app).absoluteURL(true)
              )
              .map(_ => Ok(views.html.passwordreset.forgotPasswordOk()))

          }
        })
      }
      )
    }
  }


  private val resetComplete = {
    val content: Mapping[(String, String, String, String)] = mapping(
      "password" -> nonEmptyText(8),
      "passwordRepeat" -> nonEmptyText(8),
      "email" -> nonEmptyText,
      "code" -> nonEmptyText)(Tuple4.apply)(Tuple4.unapply)

    Form(content.verifying("Les deux mots de passe ne correspondent pas", pair => pair._1 == pair._2))
  }

  private def getResetRequest(emailEnc: String, codeEnc: String): Future[Option[RegisteredUser]] = {
    val email = URLDecoder.decode(emailEnc, "UTF-8")
    val code = URLDecoder.decode(codeEnc, "UTF-8")

    users.getUser(email).flatMap {
      case Some(user) if user.passwordReset.contains(code) && user.passwordResetEnd.exists(_.after(new Date)) =>
        Some(user)
      case _ => None
    }
  }

  def passwordResetChangeGet(emailEnc: String, codeEnc: String, app: Option[String]): Action[AnyContent] = Action.async {
    implicit rq: Request[_] =>
      ExplicitTools.ifLoggedOut(app) {
        getResetRequest(emailEnc, codeEnc).map {
          case Some(_) =>
            Ok(views.html.passwordreset.changePassword(resetComplete.fill(("", "", emailEnc, codeEnc)), app))
          case None =>
            BadRequest(views.html.passwordreset.forgotPasswordNotFound())
        }
      }
  }


  def passwordResetChangePost(app: Option[String]): Action[AnyContent] = Action.async {
    implicit rq: Request[_] =>
      ExplicitTools.ifLoggedOut(app) {
        resetComplete.bindFromRequest().fold(withErrors => {
          BadRequest(views.html.passwordreset.changePassword(withErrors, app))
        }, data => {
          val (password, _, email, code) = data

          getResetRequest(email, code).flatMap {
            case Some(user) =>
              val (algo, hashPass) = hashes hash password
              val updated = user.copy(password = hashPass, passwordAlgo = algo, passwordReset = None, passwordResetEnd = None)

              users.updateUser(updated).map(_ =>
                Redirect(controllers.explicit.routes.LoginController.loginGet(app))
              )
            case None =>
              Future.successful(BadRequest(views.html.passwordreset.forgotPasswordNotFound()))
          }
        })
      }
  }


}
