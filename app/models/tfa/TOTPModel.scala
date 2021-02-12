package models.tfa

import anorm.SqlParser._
import anorm._
import com.warrenstrange.googleauth.{GoogleAuthenticator, GoogleAuthenticatorQRGenerator}
import com.yubico.webauthn.data._
import data.RegisteredUser
import models.UsersModel
import models.tfa.TFAModel.TFAMode.{TFAMode, TOTP}
import play.api.Configuration

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TOTPModel @Inject()(dbApi: play.api.db.DBApi, rpi: RelyingPartyIdentity, users: UsersModel, conf: Configuration)(implicit ec: ExecutionContext)
  extends TFARepository[Int] {
  private val db = dbApi database "default"
  private val otpIssuer = conf.getOptional[String]("totp.issuer").getOrElse("Japan Impact Auth")
  private val gAuth = new GoogleAuthenticator

  /**
   * Start enroling a new OTP device for a user
   *
   * @param username the username of the user, for display purposes
   * @return a pair (shared secret, otp configuration url). The shared secret should be sent in a session to the user,
   *         while the OTP configuration URL should be made into a QR code
   */
  def enrolStart(username: String): (String, String) = {
    val key = gAuth.createCredentials()
    val qrcode = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(otpIssuer, username, key)

    (key.getKey, qrcode)
  }

  /**
   * Complete enrolment for a user OTP device
   *
   * @param user
   * @param code
   * @param deviceName
   * @param key
   * @return
   */
  def enrolComplete(user: Int, code: Int, deviceName: String, key: String): Future[Boolean] = {
    Future {
      if (!gAuth.authorize(key, code)) false // invalid otp code
      else {
        db.withConnection { implicit c =>
          SQL"INSERT INTO totp_keys(user_id, device_name, shared_secret) VALUES ($user, $deviceName, $key)".execute()
        }

        true
      }
    }
  }

  def check(user: Int, code: Int): Future[Boolean] = {
    Future {
      db.withConnection { implicit c =>
        val keys = SQL"SELECT shared_secret FROM totp_keys WHERE user_id = $user"
          .as(str("shared_secret").*)

        keys.exists(k => gAuth.authorize(k, code)) // the code works with one of the keys retrieved from the database
      }
    }
  }

  def userHasKeys(user: Int): Future[Boolean] = Future {
    db.withConnection { implicit c =>
      SQL"SELECT * FROM totp_keys WHERE user_id = $user LIMIT 1".as(int("user_id").singleOpt).contains(user)
    }
  }

  override val mode: TFAMode = TOTP
  override val converter: Converter[Int] = new Converter[Int] {
    override def toString(id: Int): String = id.toString
    override def apply(string: String): Int = string.toInt
  }

  /**
   * Get the keys registered by the user in this mode
   *
   * @param user the user to check
   * @return a set of pairs (key name, key id)
   */
  override def getKeys(user: Int): Future[Set[(String, Int)]] = Future {
    db.withConnection { implicit c =>
      SQL"SELECT id, device_name FROM totp_keys WHERE user_id = $user"
        .as((int("id") ~ str("device_name")).*)
        .map {
          case id ~ name => (name, id)
        }
        .toSet
    }
  }

  /**
   * Remove an OTP key from a user account
   *
   * @param user the user to update
   * @param id   the id of the key to remove
   * @return
   */
  override def removeKey(user: Int, id: Int): Future[Unit] = Future {
    db.withConnection { implicit c =>
      SQL"DELETE FROM totp_keys WHERE user_id = $user AND id = $id"
        .execute()
    }
  }

  /**
   * Validates a given key
   *
   * @param user
   * @param keyData the data given by the client, to be authenticated
   * @return
   */
  override def validate(user: RegisteredUser, data: String): Future[Boolean] = {
    val code = Some(data).filter(_.length == 6).flatMap(_.toIntOption)

    if (code.isEmpty) Future.successful(false)
    else check(user.id.get, code.get)
  }
}
