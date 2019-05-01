package utils

import play.api.data.validation.{Constraints, Valid}

/**
  * @author Louis Vialar
  */
object ValidationUtils {
  def isValidEmail(email: String): Boolean = {
    if (email.isEmpty) false
    else Constraints.emailAddress.apply(email) match {
      case Valid => true
      case _ => false
    }
  }

  def isValidPassword(password: String): Boolean =
    password.length >= 8
}
