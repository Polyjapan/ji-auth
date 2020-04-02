package controllers.forms

import java.net.URLDecoder
import java.util.Date

import data.RegisteredUser
import javax.inject.Inject
import models.UsersModel
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
                                        users: UsersModel) extends MessagesAbstractController(cc) with I18nSupport {

  private val resetStart = Form(
    mapping(
      "email" -> email,
      "g-recaptcha-response" -> text)(Tuple2.apply)(Tuple2.unapply))


  def passwordResetGet: Action[AnyContent] = Action.async { implicit rq: Request[_] =>
    AuthTools.ifLoggedOut {
      Ok(views.html.passwordreset.forgotPassword(resetStart, captcha.AuthSiteKey))
    }
  }

  def passwordResetPost: Action[AnyContent] = Action.async { implicit rq: Request[_] =>
    AuthTools.ifLoggedOut {
      resetStart.bindFromRequest().fold(withErrors => {
        BadRequest(views.html.passwordreset.forgotPassword(withErrors, captcha.AuthSiteKey))
      }, data => {
        val (email, captchaResponse) = data

        // Check that the captcha is correct
        captcha.doCheckCaptchaWithExpiration(Some(captcha.AuthSecretKey), captchaResponse).flatMap(captchaResponse => {
          if (!captchaResponse.success) {
            println(captchaResponse)
            BadRequest(views.html.passwordreset.forgotPassword(resetStart.withGlobalError("Captcha incorrect"), config.get[String]("recaptcha.siteKey")))
          }
          else {
            users
              .resetPassword(email, (email, code) =>
                controllers.forms.routes.PasswordResetController.passwordResetChangeGet(email, code).absoluteURL(true)
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

  def passwordResetChangeGet(emailEnc: String, codeEnc: String): Action[AnyContent] = Action.async {
    implicit rq: Request[_] =>
      AuthTools.ifLoggedOut {
        getResetRequest(emailEnc, codeEnc).map {
          case Some(_) =>
            Ok(views.html.passwordreset.changePassword(resetComplete.fill(("", "", emailEnc, codeEnc))))
          case None =>
            BadRequest(views.html.passwordreset.forgotPasswordNotFound())
        }
      }
  }


  def passwordResetChangePost: Action[AnyContent] = Action.async {
    implicit rq: Request[_] =>
      AuthTools.ifLoggedOut {
        resetComplete.bindFromRequest().fold(withErrors => {
          BadRequest(views.html.passwordreset.changePassword(withErrors))
        }, data => {
          val (password, _, email, code) = data

          getResetRequest(email, code).flatMap {
            case Some(user) =>
              val (algo, hashPass) = hashes hash password
              val updated = user.copy(password = hashPass, passwordAlgo = algo, passwordReset = None, passwordResetEnd = None)

              users.updateUser(updated).map(_ =>
                Redirect(controllers.forms.routes.LoginController.loginGet(None, None))
              )
            case None =>
              Future.successful(BadRequest(views.html.passwordreset.forgotPasswordNotFound()))
          }
        })
      }
  }


}
