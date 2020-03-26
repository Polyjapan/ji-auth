package utils

import java.security.SecureRandom
import java.util.Base64

import data.SessionID

import scala.util.Random

/**
  * @author zyuiop
  */
object RandomUtils {
  private val base64 = Base64.getUrlEncoder
  private val random = new Random(new SecureRandom())

  def randomString(bytes: Int): String = {
    base64.encodeToString(randomBytes(bytes)).filterNot(_ == '=')
  }

  def randomBytes(bytes: Int): Array[Byte] = {
    val tokenBytes = new Array[Byte](bytes)
    random.nextBytes(tokenBytes)
    tokenBytes
  }

  def randomSession: SessionID = SessionID(randomBytes(16))
}
