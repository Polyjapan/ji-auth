package services

import java.util.Date

import javax.inject.Inject
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author zyuiop
  */
class ReCaptchaService @Inject()(ws: WSClient)(implicit ec: ExecutionContext, config: Configuration) {

  lazy val AuthSiteKey: String = config.get[String]("recaptcha.siteKey")
  lazy val AuthSecretKey: String = config.get[String]("recaptcha.secretKey")

  def doCheckCaptcha(optKey: Option[String], response: String): Future[ReCaptchaResponse] = optKey match {
    case Some(secretKey) =>
      val params = Map[String, String]("secret" -> secretKey, "response" -> response)
      implicit val tsreads: Reads[DateTime] = Reads.of[String] map (new DateTime(_))
      implicit val tswrites: Writes[DateTime] = Writes { dt: DateTime => JsString(dt.toString) }

      implicit val responseFormat: OFormat[ReCaptchaResponse] = Json.format[ReCaptchaResponse]

      ws.url("https://www.google.com/recaptcha/api/siteverify").post(params).map(resp => {
        resp.json.as[ReCaptchaResponse]
      })

    case None => Future(ReCaptchaResponse(true, Some(new DateTime(System.currentTimeMillis())), Some("nocaptcha.void")))
  }

  def doCheckCaptchaWithExpiration(secretKey: Option[String], response: String, hours: Int = 6): Future[ReCaptchaResponse] = {
    val nowMinus6 = new Date(System.currentTimeMillis() - (6 * 60 * 60 * 1000))

    doCheckCaptcha(secretKey, response).map(r => {
      if (r.success && r.challenge_ts.get.toDate.before(nowMinus6)) r.copy(success = false)
      else r
    })
  }

  case class ReCaptchaResponse(success: Boolean, challenge_ts: Option[DateTime], hostname: Option[String]) // We can get the errors too
}

