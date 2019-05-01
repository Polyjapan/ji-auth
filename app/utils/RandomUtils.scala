package utils

import java.security.SecureRandom
import java.util.Base64

import scala.util.Random

/**
  * @author zyuiop
  */
object RandomUtils {
  private val base64 = Base64.getUrlEncoder
  private val random = new Random(new SecureRandom())

  def randomString(bytes: Int): String = {
    val tokenBytes = new Array[Byte](bytes)
    random.nextBytes(tokenBytes)

    base64.encodeToString(tokenBytes).filterNot(_ == '=')
  }
}
