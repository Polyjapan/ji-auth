package utils

import play.api.data.Mapping
import play.api.data.validation.{Constraints, Valid}

/**
  * @author Louis Vialar
  */
object ValidationUtils {
  val PhoneRegex = "^[+]*[(]{0,1}[0-9]{1,4}[)]{0,1}[-\\s./0-9]*$"

  def isValidPhone(phone: String): Boolean = phone.matches(PhoneRegex)

  def validPhoneVerifier(phone: Mapping[String]): Mapping[String] = {
    phone.verifying("Numéro invalide.", p => ValidationUtils.isValidPhone(p))
  }

  def isValidEmail(email: String): Boolean = {
    if (email.isEmpty || email.length > 180) false
    else Constraints.emailAddress.apply(email) match {
      case Valid => true
      case _ => false
    }
  }

  def isValidPassword(password: String): Boolean =
    password.length >= 8
}
