package models.tfa

import data.RegisteredUser
import models.tfa.TFAModel.TFAMode.TFAMode

import scala.concurrent.{ExecutionContext, Future}

trait Converter[IDType] {
  def toString(id: IDType): String
  def apply(string: String): IDType
}

/**
 * Represents a repository for TFA parameters
 *
 * @tparam IDType the type used for the id column of the keys
 */
trait TFARepository[IDType] {
  val mode: TFAMode

  val converter: Converter[IDType]

  /**
   * Check if a given user has keys in this mode (i.e. if this mode is enabled for that user)
   *
   * @param user the user to check
   * @return a future, with true if this mode is enabled for the given user
   */
  def userHasKeys(user: Int): Future[Boolean]

  /**
   * Get the keys registered by the user in this mode
   *
   * @param user the user to check
   * @return a set of pairs (key name, key id)
   */
  def getKeys(user: Int): Future[Set[(String, IDType)]]

  def getKeysString(user: Int)(implicit ec: ExecutionContext): Future[Set[(String, String)]] =
    getKeys(user).map(_.map { case (name, key) => (name, converter.toString(key)) })

  /**
   * Remove an OTP key from a user account
   *
   * @param user the user to update
   * @param id   the id of the key to remove
   * @return
   */
  def removeKey(user: Int, id: IDType): Future[Unit]

  def removeKeyString(user: Int, id: String): Future[Unit] = removeKey(user, converter(id))

  /**
   * Validates a given key
   * @param user
   * @param keyData the data given by the client, to be authenticated
   * @return
   */
  def validate(user: RegisteredUser, keyData: String): Future[Boolean]
}
