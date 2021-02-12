package models

import models.TFAModel.TFAMode.{TFAMode, WebAuthn}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object TFAModel {
  object TFAMode extends Enumeration {
    type TFAMode = Value
    val WebAuthn, TOTP, Backup = Value

    def fromString(s: String): Option[TFAMode] = values.find(_.toString == s)
  }
}

class TFAModel @Inject()(webAuthnModel: WebAuthnModel)(implicit ec: ExecutionContext) {
  private val tfaModesCheck: Map[TFAModel.TFAMode.Value, Int => Future[Boolean]] = Map(
    WebAuthn -> webAuthnModel.userHasKeys
  )


  /**
   * Gets the set of supported TFA modes for a user. If the returned set is not empty, TFA **must** be checked
   * for that user
   * @param userId the id of the user to check
   * @return a future with a list of TFA modes
   */
  def tfaModes(userId: Int): Future[Set[TFAMode]] = {
    (Future.sequence {
      tfaModesCheck.map {
        case (mode, func) => func(userId).map(res => if (res) Some(mode) else None)
      }
    }).map(_.flatten.toSet)
  }

}
