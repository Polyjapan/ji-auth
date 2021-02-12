package controllers.forms

import data.UserSession
import models.{ServicesModel, SessionsModel, UsersModel, WebAuthnModel}
import play.Logger
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsObject, JsResult, JsValue}
import play.api.libs.mailer.MailerClient
import play.api.mvc._
import play.twirl.api.HtmlFormat

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * @author Louis Vialar
 */
class WebAuthnManagementController @Inject()(cc: MessagesControllerComponents)(implicit ec: ExecutionContext,
                                                                               users: UsersModel,
                                                                               webAuthn: WebAuthnModel) extends MessagesAbstractController(cc) with I18nSupport {

  def registerGet: Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedIn { user: UserSession =>
      Future.successful(Ok(views.html.webauthn.register()))
    }
  }

  def registerGetParams: Action[AnyContent] = Action.async { implicit rq =>
    AuthTools.ifLoggedIn { user: UserSession =>
      users.getUserById(user.id).flatMap {
        case Some(user) => webAuthn.startRegistration(user)
        case None => Future.failed(new NullPointerException)
      }.map {
        case (json, uid) => Ok(s"""{ "uid":"${uid.toString}", "pk": $json }""").as("text/json")
      }
    }
  }

  def registerComplete: Action[JsValue] = Action.async(parse.json) { implicit rq =>
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
