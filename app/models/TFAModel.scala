package models

import models.TFAModel.TFAMode.{TFAMode, WebAuthn, TOTP, Backup}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object TFAModel {
  object TFAMode extends Enumeration {
    type TFAMode = Value
    val WebAuthn, TOTP, Backup = Value

    def fromString(s: String): Option[TFAMode] = values.find(_.toString == s)
  }
}

class TFAModel @Inject()(webAuthnModel: WebAuthnModel, totpModel: TOTPModel)(implicit ec: ExecutionContext) {
  private val mapping: Map[TFAMode, TFARepository[_]] = Map(WebAuthn -> webAuthnModel, TOTP -> totpModel)

  def repository(mode: TFAMode): TFARepository[_] = mapping(mode)

  /**
   * Gets the set of supported TFA modes for a user. If the returned set is not empty, TFA **must** be checked
   * for that user
   * @param userId the id of the user to check
   * @return a future with a list of TFA modes
   */
  def tfaModes(userId: Int): Future[Set[TFAMode]] = {
    Future.sequence {
      mapping.map {
        case (mode, repo) => repo.userHasKeys(userId).map(res => if (res) Some(mode) else None)
      }
    }.map(_.flatten.toSet)
  }

  /**
   *
   * @param userId
   * @return a future with a set of tuples (TFA mode, name of the device, string representation of the ID)
   */
  def tfaKeys(userId: Int): Future[Set[(TFAMode, String, String)]] = {
    Future.sequence {
      mapping.map {
        case (mode, repo) => repo.getKeysString(userId)
          .map(set => set.map { case (name, id) => (mode, name, id) })
      }
    }.map(_.flatten.toSet)
  }
}
