package models

import javax.inject.Singleton
import javax.xml.bind.DatatypeConverter
import org.bouncycastle.jcajce.provider.digest.{MD5, SHA1}
import org.mindrot.jbcrypt.BCrypt

/**
  * @author zyuiop
  */
@Singleton
class HashModel {
  private val providers: Map[String, HashProvider] = Map(
    "bcrypt" -> new BCryptHashProvider,
    "old" -> new ShittyAlgoProvider
  )
  val DefaultAlgo = "bcrypt"
  private val RandomHashed = hash("this is a random string")._2


  /**
    * Hashes the given password with the default algorithm
    *
    * @param password the password to hash
    * @return a tuple (algo, hash)
    */
  def hash(password: String): (String, String) = (DefaultAlgo, hash(DefaultAlgo, password))

  def hash(algo: String, password: String): String = providers(algo) hash password

  def check(algo: String, hashed: String, input: String): Boolean = providers(algo).check(hashed, input)

  /**
    * Check the password against a constant password, to counter timing attacks
    */
  def fakeCheck(pass: String): Unit = {
    check(DefaultAlgo, RandomHashed, pass)
  }

  def upgrade(algo: String, clearPassword: String): Option[(String, String)] = {
    if (algo != DefaultAlgo) Some(hash(clearPassword))
    else Option.empty
  }

  private trait HashProvider {
    def hash(password: String): String

    def check(hashed: String, input: String): Boolean
  }

  private class BCryptHashProvider extends HashProvider {
    override def hash(password: String): String = {
      if (password != null) BCrypt.hashpw(password, BCrypt.gensalt())
      else throw new NullPointerException
    }

    override def check(hashed: String, input: String): Boolean = {
      if (hashed != null && input != null) BCrypt.checkpw(input, hashed)
      else throw new NullPointerException
    }
  }

  /**
    * The HashProvider for the old password hashing mechanism
    */
  private class ShittyAlgoProvider extends HashProvider {
    override def hash(password: String): String = throw new UnsupportedOperationException("why are you using this algo?")

    override def check(hashed: String, input: String): Boolean = {
      val sha = new SHA1.Digest
      val md5 = new MD5.Digest
      sha.update(input.trim.getBytes)
      val salt = DatatypeConverter.printHexBinary(sha.digest()).toLowerCase

      md5.update(input.trim.getBytes)
      md5.update(salt.getBytes)

      val hash = DatatypeConverter.printHexBinary(md5.digest()).toLowerCase

      hash == hashed
    }
  }

}
