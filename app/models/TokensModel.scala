package models

import java.security.SecureRandom
import java.sql.Timestamp
import java.util.Base64

import data._
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author zyuiop
  */
class TokensModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  val rand = new SecureRandom()

  import profile.api._

  def createTokenForUser(client: Int, userAgent: String, userIp: String): Future[String] = {
    val base64 = Base64.getUrlEncoder
    val tokenBytes = new Array[Byte](16) // 16 bytes should be enough entropy
    rand.nextBytes(tokenBytes)
    val tokenString = base64.encodeToString(tokenBytes)

    val time = new Timestamp(System.currentTimeMillis())
    val endTime = new Timestamp(System.currentTimeMillis() + (180 * 24 * 3600 * 1000L))

    val token = RefreshToken(tokenString, client, time, endTime, userAgent, time, userIp)

    db.run(refreshTokens returning refreshTokens.map(_.token) += token)
  }

}
