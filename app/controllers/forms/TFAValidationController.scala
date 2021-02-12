package controllers.forms

import controllers.forms.TFAValidationController.readTemporarySession
import data.{RegisteredUser, UserSession}
import models.TFAModel.TFAMode.{TFAMode, WebAuthn}
import models.{SessionsModel, TFAModel, UsersModel, WebAuthnModel}
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


/**
 * @author Louis Vialar
 */
class TFAValidationController @Inject()(cc: MessagesControllerComponents)(implicit ec: ExecutionContext,
                                                                          users: UsersModel,
                                                                          sessions: SessionsModel,
                                                                          tfa: TFAModel,
                                                                          webAuthn: WebAuthnModel) extends MessagesAbstractController(cc) with I18nSupport {


  private val tfaLoginForm = Form(mapping("type" -> nonEmptyText, "data" -> nonEmptyText)(Tuple2.apply)(Tuple2.unapply))
  private val validators: Map[TFAMode, (RegisteredUser, String) => Future[Boolean]] = Map(
    WebAuthn -> validateWebauthn
  )

  def tfaCheckGet: Action[AnyContent] = Action.async { implicit rq =>
    readTemporarySession match {
      case Some(userId) =>
        tfa.tfaModes(userId).flatMap {
          case modes if modes.nonEmpty =>
            Future.successful(Ok(views.html.tfa.login(modes)))
          case _ => completeLogin(userId) // No TFA enabled
        }

      case None => Future.successful(Redirect(controllers.forms.routes.LoginController.loginGet()))
    }

  }

  private def completeLogin(userId: Int)(implicit rq: RequestHeader): Future[Result] = {
    users.getUserById(userId).flatMap {
      case Some(user) => completeLogin(user)
      case None => Future.successful(BadRequest("Bad userID in session."))
    }
  }

  private def completeLogin(user: RegisteredUser)(implicit rq: RequestHeader): Future[Result] = {
    sessions.createSession(user.id.get, rq.remoteAddress, rq.headers.get("User-Agent").getOrElse("unknown"))
      .map(sid => Redirect(controllers.routes.RedirectController.redirectGet()).addingToSession(UserSession(user, sid): _*))
  }

  def tfaCheckPost: Action[(String, String)] = Action.async(parse.form(tfaLoginForm)) { implicit rq =>
    readTemporarySession match {
      case Some(userId) =>
        tfa.tfaModes(userId).flatMap {
          case modes if modes.nonEmpty =>
            val tfaType = TFAModel.TFAMode.fromString(rq.body._1)
            val data = rq.body._2

            if (tfaType.isEmpty) {
              println("User " + userId + " tried to use invalid tfa mode " + rq.body._1)
              Future.successful(Redirect(controllers.forms.routes.TFAValidationController.tfaCheckGet()))
            } else if (!modes(tfaType.get)) {
              println("User " + userId + " tried to use TFA mode " + tfaType.get + " but only " + modes + " are configured.")
              Future.successful(Redirect(controllers.forms.routes.TFAValidationController.tfaCheckGet()))
            } else {
              users.getUserById(userId).flatMap {
                case Some(user) =>
                  validators(tfaType.get)(user, data).flatMap {
                    case true =>
                      completeLogin(user)
                    case false =>
                      // TODO: When implementing TOTP, think about rate limits
                      // since in TOTP you usually have a window of +- 30 seconds to use a token, it means you have 90sec
                      // to bruteforce. On average, you find the correct combination after N/2 attempts [conservative estimate]
                      // Therefore, an attacker needs to try 10k comb/sec to have a 50% chance to win
                      // unrealistic attack rate, should work (requires 10 attacks per milliseconds... you have denial
                      // of service way before that threshold, and we'll never have google-like infra anyway!)
                      Future.successful(Redirect(controllers.forms.routes.TFAValidationController.tfaCheckGet())
                        .flashing("error" -> "Échec de la vérification, merci de réessayer"))
                  }
                case None =>
                  Future.successful(BadRequest("Bad userID in session."))
              }
            }
        }
      case None => Future.successful(Redirect(controllers.forms.routes.LoginController.loginGet()))
    }
  }

  /**
   * Endpoint called via AJAX to init a WebAuthn challenge
   */
  def webauthnInit: Action[AnyContent] = Action.async { implicit rq =>
    readTemporarySession match {
      case Some(userId) =>
        users.getUserById(userId).flatMap {
          case Some(user) =>
            webAuthn.startAuthentication(user)
              .map {
                case (json, uid) => Ok(s"""{ "uid":"${uid.toString}", "pk": $json }""").as("text/json")
              }

          case None =>
            Future.successful(BadRequest("Bad userID in session"))
        }

      case None => Future.successful(BadRequest("No user ID found"))
    }

  }

  private def validateWebauthn(user: RegisteredUser, data: String): Future[Boolean] = {
    val json = Json.parse(data)
    val uid: Option[UUID] = (json \ "uid").validate[String].asOpt.flatMap(str => Try(UUID.fromString(str)).toOption)
    val response: Option[JsObject] = (json \ "pk").validate[JsObject].asOpt

    if (uid.isEmpty || response.isEmpty) {
      println("User " + user.id.get + " tried to use WebAuthn but did not provide correct parameters")
      Future.successful(false)
    } else {
      webAuthn.validateAuthentication(user, uid.get, response.get)
    }
  }
}

object TFAValidationController {

  import scala.concurrent.duration._

  private val TemporarySessionName = "tempUid"
  private val TemporarySessionExpName = "tempUidExp"
  private val TemporarySessionDuration = 10.minutes.toMillis

  def writeTemporarySession(userId: Int)(result: Result)(implicit requestHeader: RequestHeader): Result = {
    result.addingToSession(
      TemporarySessionName -> userId.toString,
      TemporarySessionExpName -> (System.currentTimeMillis() + TemporarySessionDuration).toString
    )
  }

  def readTemporarySession(implicit requestHeader: RequestHeader): Option[Int] = {
    val userId = requestHeader.session.get(TemporarySessionName).flatMap(_.toIntOption)
    val isValid = requestHeader.session.get(TemporarySessionExpName).flatMap(_.toLongOption)
      .exists(_ > System.currentTimeMillis())

    if (isValid) userId else None
  }
}