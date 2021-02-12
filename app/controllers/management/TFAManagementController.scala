package controllers.management

import controllers.forms.AuthTools
import data.UserSession
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc._
import play.filters.csrf.CSRFCheck

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * @author Louis Vialar
 */
class TFAManagementController @Inject()(cc: MessagesControllerComponents, checkToken: CSRFCheck)(implicit ec: ExecutionContext,
                                                                                                 users: UsersModel,
                                                                                                 webAuthn: WebAuthnModel,
                                                                                                 tfa: TFAModel, backups: BackupCodesModel,
                                                                                                 totp: TOTPModel) extends MessagesAbstractController(cc) with I18nSupport {

  private val OTPKeySessionKey = "otpKey"
  private val OTPUrlSessionKey = "otpUrl"
  private val ErrorFlashKey = "error"

  private val otpEnrolForm = Form(mapping("code" -> text(6, 6).verifying(_.forall(_.isDigit)).transform[Int](_.toInt, _.toString), "name" -> nonEmptyText)(Tuple2.apply)(Tuple2.unapply))
  private val tfaDeleteForm = Form(mapping("type" -> nonEmptyText, "id" -> nonEmptyText)(Tuple2.apply)(Tuple2.unapply))

  def get: Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedIn { user: UserSession =>
      tfa.tfaModes(user.id).flatMap { modes =>
        tfa.tfaKeys(user.id).map { keys =>
          Ok(views.html.tfa.manage(modes, keys))
        }
      }
    }
  }

  def deleteKey: Action[(String, String)] = Action.async(parse.form(tfaDeleteForm)) { implicit rq =>
    AuthTools.ifLoggedIn { user: UserSession =>
      val tfaType = TFAModel.TFAMode.fromString(rq.body._1)
      val id = rq.body._2

      if (tfaType.isEmpty) Future.successful(Redirect(controllers.management.routes.TFAManagementController.get()))
      else {
        tfa.repository(tfaType.get).removeKeyString(user.id, id).map(_ => Redirect(controllers.management.routes.TFAManagementController.get()))
      }
    }
  }

  def generateBackupCodes: Action[AnyContent] = checkToken {
    Action.async { implicit rq =>
      AuthTools.ifLoggedIn { user: UserSession =>
        backups.generate(user.id).map { codes =>
          Ok(views.html.tfa.backup_generate(codes))
        }
      }
    }
  }

  def totpEnrolStart: Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedIn { user =>
      val (key, url) = (rq.session.get(OTPKeySessionKey), rq.session.get(OTPUrlSessionKey)) match {
        case (Some(k), Some(u)) => (k, u)
        case _ => totp.enrolStart(user.email)
      }

      Future.successful(
        Ok(views.html.tfa.otp_enrol(key, url, rq.flash.get(ErrorFlashKey), otpEnrolForm)).addingToSession(OTPKeySessionKey -> key, OTPUrlSessionKey -> url))
    }
  }

  def totpEnrolFinish: Action[(Int, String)] = Action.async(parse.form(otpEnrolForm)) { implicit rq =>
    AuthTools.ifLoggedIn { user =>
      rq.session.get(OTPKeySessionKey) match {
        case Some(key) =>
          val (code, name) = rq.body
          totp.enrolComplete(user.id, code, name, key).map {
            case true =>
              Redirect(controllers.management.routes.TFAManagementController.get()).removingFromSession(OTPKeySessionKey, OTPUrlSessionKey)
            case false =>
              Redirect(controllers.management.routes.TFAManagementController.totpEnrolStart()).flashing(ErrorFlashKey -> "Code entré invalide")
          }
        case _ => Future.successful(Redirect(controllers.management.routes.TFAManagementController.totpEnrolStart()))
      }
    }
  }

  def webauthnGetParam: Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedIn { user: UserSession =>
      users.getUserById(user.id).flatMap {
        case Some(user) => webAuthn.startRegistration(user)
        case None => Future.failed(new NullPointerException)
      }.map {
        case (json, uid) => Ok(s"""{ "uid":"${uid.toString}", "pk": $json }""").as("text/json")
      }
    }
  }

  def webauthnCompleteRegistration: Action[JsValue] = Action.async(parse.json) { implicit rq =>
    AuthTools.ifLoggedIn { user: UserSession =>
      val uid: Option[UUID] = (rq.body \ "uid").validate[String].asOpt.flatMap(str => Try(UUID.fromString(str)).toOption)
      val name: Option[String] = (rq.body \ "name").validate[String].asOpt
      val response: Option[JsObject] = (rq.body \ "pk").validate[JsObject].asOpt

      val res = (uid, name, response) match {
        case (Some(u), Some(n), Some(r)) =>

          users.getUserById(user.id).flatMap {
            case Some(user) => webAuthn.finishRegistration(user, u, r, n)
            case None =>
              println("Missing user data")
              Future.successful(false)
          }
        case _ =>
          println("Missing parameter in JSON body")
          Future.successful(false)
      }

      res.map {
        case true => Ok
        case false => BadRequest
      }
    }
  }


}
