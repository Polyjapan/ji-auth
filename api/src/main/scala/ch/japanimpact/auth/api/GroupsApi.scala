package ch.japanimpact.auth.api

import akka.Done
import ch.japanimpact.api.APIError
import com.google.inject.ImplementedBy

import scala.concurrent.Future

@ImplementedBy(classOf[HttpGroupsApi])
trait GroupsApi {
  type Result[A] = Future[Either[APIError, A]]

  def getGroups: Result[Seq[GroupData]]

  def createGroup(group: Group): Result[Option[Group]]

  def apply(groupName: String): GroupApi

  def group(groupName: String): GroupApi = apply(groupName)

  trait GroupApi {
    val name: String

    def update(group: Group): Result[Done]

    def get: Result[GroupData]

    def delete: Result[Done]

    def addScope(scope: String): Result[Done]

    def deleteScope(scope: String): Result[Done]

    def addMember(user: Int): Result[Done]

    def deleteMember(user: Int): Result[Done]

    def getMembers: Result[Seq[UserProfile]]
  }
}
