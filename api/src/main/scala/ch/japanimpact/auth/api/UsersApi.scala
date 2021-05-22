package ch.japanimpact.auth.api

import akka.Done
import ch.japanimpact.api.APIError
import com.google.inject.ImplementedBy
import play.api.libs.json.Json

import scala.concurrent.Future

@ImplementedBy(classOf[HttpUsersApi])
trait UsersApi {
  type Result[A] = Future[Either[APIError, A]]

  def getUsers: Result[Iterable[UserProfile]]

  def searchUsers(query: String): Result[Seq[UserProfile]]

  def getUsersWithIds(ids: Set[Int]): Result[PartialFunction[Int, UserData]]

  def user(userId: Int): UserApi

  def apply(userId: Int): UserApi = user(userId)

  trait UserApi {
    val userId: Int

    def get: Result[UserData]

    def forceLogOut(): Result[Done]

    def forceConfirmEmail(): Result[Done]

    def addScope(scope: String): Result[Done]

    def removeScope(scope: String): Result[Done]

    def update(profile: UserProfile): Result[Done]
  }
}
