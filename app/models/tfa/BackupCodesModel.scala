package models.tfa

import anorm.SqlParser._
import anorm._
import com.yubico.webauthn.data._
import models.UsersModel
import models.tfa.TFAModel.TFAMode.{TFAMode, Backup}
import play.api.Configuration

import java.security.SecureRandom
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BackupCodesModel @Inject()(dbApi: play.api.db.DBApi, rpi: RelyingPartyIdentity, users: UsersModel, conf: Configuration)(implicit ec: ExecutionContext)
  extends TFARepository[Unit] {

  override val mode: TFAMode = Backup
  override val converter: Converter[Unit] = new Converter[Unit] {
    override def toString(id: Unit): String = "Unit"
    override def apply(string: String): Unit = ()
  }
  private val db = dbApi database "default"
  // TODO: may move this to config eventually
  private val NCodes = 10
  private val CodeLength = 20 // in hexadecimal characters [<=> *4 for number of bits] ; PLEASE STAY AT A MULTIPLE OF TWO
  private val CodeSpacing = 5
  private val Random = new SecureRandom()
  private val Separator = "-"

  def generate(user: Int): Future[Set[String]] = {
    val keys = for (i <- 1 to NCodes) yield {
      val arr = new Array[Byte](CodeLength / 2)
      Random.nextBytes(arr)
      arr.map("%02X".format(_)).mkString.grouped(CodeSpacing).mkString(Separator).toUpperCase
    }

    // First, remove previous code
    removeKey(user, ()).flatMap(_ => Future {
      db.withConnection { implicit c =>
        val named = keys.map(key => Seq[NamedParameter]("uid" -> user, "code" -> key))

        BatchSql("INSERT INTO tfa_backup_codes(user_id, code) VALUES ({uid}, {code})", named.head, named.tail: _*)
          .execute()

        keys.toSet
      }
    })
  }

  def check(user: Int, key: String): Future[Boolean] = Future {
    db.withConnection { implicit c =>
      val result = SQL"SELECT code FROM tfa_backup_codes WHERE user_id = $user AND code = $key LIMIT 1".as(str("code").singleOpt).contains(key)

      if (result) {
        SQL"DELETE FROM tfa_backup_codes WHERE user_id = $user AND code = $key"
          .execute()
      }

      result
    }
  }

  def userHasKeys(user: Int): Future[Boolean] = Future {
    db.withConnection { implicit c =>
      SQL"SELECT * FROM tfa_backup_codes WHERE user_id = $user LIMIT 1".as(int("user_id").singleOpt).contains(user)
    }
  }

  /**
   * Get the keys registered by the user in this mode
   *
   * @param user the user to check
   * @return a set of pairs (key name, key id)
   */
  override def getKeys(user: Int): Future[Set[(String, Unit)]] =
    userHasKeys(user).map {
      case true => Set("Codes de secours" -> ())
      case _ => Set()
    }

  /**
   * Remove an OTP key from a user account
   *
   * @param user the user to update
   * @param id   the id of the key to remove
   * @return
   */
  override def removeKey(user: Int, id: Unit): Future[Unit] = Future {
    db.withConnection { implicit c =>
      SQL"DELETE FROM tfa_backup_codes WHERE user_id = $user"
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
  override def validate(user: data.RegisteredUser, keyData: String): Future[Boolean] = check(user.id.get, keyData.trim.toUpperCase)
}
