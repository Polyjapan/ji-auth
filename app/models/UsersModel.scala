package models

import com.google.common.base.Preconditions
import javax.inject.Inject
import data.RegisteredUser
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author zyuiop
  */
class UsersModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[MySQLProfile] {

  import profile.api._

  /**
    * Gets a user in the database by its email
    * @param email the email of the user to get
    * @return a future optional user (Some(user) if found, None if not)
    */
  def getUser(email: String): Future[Option[RegisteredUser]] =
    db.run(registeredUsers.filter(_.email === email).result.headOption)

  /**
    * Create a user
    * @param user the user to create
    * @return a future hodling the id of the inserted user
    */
  def createUser(user: RegisteredUser): Future[Int] =
    db.run((registeredUsers returning registeredUsers.map(_.id)) += user)

  /**
    * Updates a user whose id is set
    * @param user the user to update/insert
    * @return the number of updated rows in a future
    */
  def updateUser(user: RegisteredUser): Future[Int] = {
    Preconditions.checkArgument(user.id.isDefined)
    db.run(registeredUsers.filter(_.id === user.id.get).update(user))
  }
}
