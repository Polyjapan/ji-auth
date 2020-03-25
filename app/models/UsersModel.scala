package models

import java.net.URLEncoder
import java.sql.Timestamp

import ch.japanimpact.auth.api.constants.GeneralErrorCodes.InvalidCaptcha
import com.google.common.base.Preconditions
import data.{Address, RegisteredUser}
import javax.inject.Inject
import play.api.Configuration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.i18n.MessagesProvider
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc.MessagesRequestHeader
import services.{HashService, ReCaptchaService}
import slick.jdbc.MySQLProfile
import utils.Implicits._
import utils.RandomUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author zyuiop
  */
class UsersModel @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, mailer: MailerClient, reCaptcha: ReCaptchaService, hashes: HashService)(implicit ec: ExecutionContext, config: Configuration)
  extends HasDatabaseConfigProvider[MySQLProfile] {


  import profile.api._

  /**
    * Gets a user in the database by its email
    *
    * @param email the email of the user to get
    * @return a future optional user (Some(user) if found, None if not)
    */
  def getUser(email: String): Future[Option[RegisteredUser]] =
    db.run(registeredUsers.filter(_.email === email).result.headOption)

  def getUserProfile(id: Int): Future[Option[(RegisteredUser, Option[Address])]] =
    db.run(registeredUsers.filter(_.id === id).joinLeft(addresses).on(_.id === _.id).result.headOption)

  def getUserProfiles(ids: Set[Int]): Future[Map[Int, (RegisteredUser, Option[Address])]] =
    db.run(registeredUsers
      .filter(_.id.inSet(ids))
      .joinLeft(addresses)
      .on(_.id === _.id).result
    ).map(_.groupBy(_._1.id.get).view.mapValues(_.head).toMap)

  /**
    * Gets a user in the database by its id
    *
    * @param id the id of the user to get
    * @return a future optional user (Some(user) if found, None if not)
    */
  def getUserById(id: Int): Future[Option[RegisteredUser]] =
    db.run(registeredUsers.filter(_.id === id).result.headOption)

  /**
    * Create a user
    *
    * @param user the user to create
    * @return a future hodling the id of the inserted user
    */
  def createUser(user: RegisteredUser, address: Option[Address]): Future[Int] =
    db.run((registeredUsers returning registeredUsers.map(_.id)) += user)
      .flatMap(id =>
        if (address.nonEmpty) db.run(addresses += address.get.copy(userId = id)).map(_ => id)
        else Future.successful(id)
      )

  def setAddressAndPhone(addr: Address, phone: String): Future[Boolean] = {
    db.run((addresses += addr) andThen (registeredUsers.filter(_.id === addr.userId).map(_.phoneNumber).update(Some(phone)))).flatMap(_ > 0)
  }


  /**
    * Updates a user whose id is set
    *
    * @param user the user to update/insert
    * @return the number of updated rows in a future
    */
  def updateUser(user: RegisteredUser): Future[Int] = {
    Preconditions.checkArgument(user.id.isDefined)
    db.run(registeredUsers.filter(_.id === user.id.get).update(user))
  }

  def update(id: Int, firstName: String, lastName: String, phone: String, addr: Address): Future[Boolean] = {
    db.run(
      registeredUsers.filter(_.id === id).map(u => (u.firstName, u.lastName, u.phoneNumber))
        .update((firstName, lastName, Some(phone))) >> addresses.insertOrUpdate(addr)
    ).flatMap(_ > 0)
  }

  def searchUsers(searchString: String): Future[Seq[RegisteredUser]] = {
    if (searchString.length < 3) Future.successful(Seq.empty)
    else db.run(
      registeredUsers.filter(u =>
        (u.email like s"%$searchString%") || (u.firstName like s"%$searchString%") || (u.lastName like s"%$searchString%")
      ).take(10).result
    )
  }

  sealed trait RegisterResult

  /**
    * The captcha was not correct
    */
  case object BadCaptcha extends RegisterResult

  /**
    * There was already an existing user with this id
    *
    * @param id the id of the existing user
    */
  case class AlreadyRegistered(id: Int) extends RegisterResult

  /**
    * The user was created with the following id
    *
    * @param id the id of the created user
    */
  case class AccountCreated(id: Int) extends RegisterResult

  /**
    * Register a new user to the system
    *
    * @param captcha             the captcha value entered
    * @param captchaPrivateKey   the private key for recaptcha
    * @param emailConfirmBuilder a function that produces the email confirm url: (email, code) => url
    * @param rq                  the request
    * @return a future with a [[RegisterResult]]
    */
  def register(captcha: String,
               captchaPrivateKey: Option[String],
               user: RegisteredUser,
               address: Option[Address],
               emailConfirmBuilder: (String, String) => String
              )(implicit rq: MessagesRequestHeader): Future[RegisterResult] = {

    reCaptcha.doCheckCaptchaWithExpiration(captchaPrivateKey, captcha).flatMap(result =>
      if (!result.success) {
        BadCaptcha
      } else {
        getUser(user.email).flatMap {
          case Some(c) =>
            mailer.send(Email(
              rq.messages("users.register.exists.email_title"),
              rq.messages("users.register.exists.email_from") + " <noreply@japan-impact.ch>",
              Seq(user.email),
              bodyText = Some(rq.messages("users.register.exists.email_text"))
            ))

            AlreadyRegistered(c.id.get)
          case None =>

            val confirmCode = RandomUtils.randomString(32)
            val emailEncoded = URLEncoder.encode(user.email, "UTF-8")

            val url = emailConfirmBuilder(emailEncoded, URLEncoder.encode(confirmCode, "UTF-8"))
            val (algo, hash) = hashes.hash(user.password)

            mailer.send(Email(
              rq.messages("users.register.email_title"),
              rq.messages("users.register.email_from") + " <noreply@japan-impact.ch>",
              Seq(user.email),
              bodyText = Some(rq.messages("users.register.email_text", url))
            ))

            createUser(
              user.copy(password = hash, passwordAlgo = algo, emailConfirmKey = Some(confirmCode)),
              address
            )
              .map(AccountCreated)
        }
      }
    )
  }

  def resetPassword(email: String, urlBuilder: (String, String) => String)(implicit rq: MessagesProvider) = {
    getUser(email).map {

      // Get the client
      case Some(user) =>

        val resetCode = RandomUtils.randomString(32)
        val resetCodeEncoded = URLEncoder.encode(resetCode, "UTF-8")
        val emailEncoded = URLEncoder.encode(user.email, "UTF-8")

        val url = urlBuilder(emailEncoded, resetCodeEncoded)

        // Update the user with the code, and a validity expiration of 24hrs
        updateUser(user.copy(
          passwordReset = Some(resetCode),
          passwordResetEnd = Some(new Timestamp(System.currentTimeMillis + (24 * 3600 * 1000)))))
          .onComplete(_ => {
            mailer.send(Email(
              rq.messages("users.recover.email_title"),
              rq.messages("users.recover.email_from") + " <noreply@japan-impact.ch>",
              Seq(user.email),
              bodyText = Some(rq.messages("users.recover.email_text", url))
            ))
          })
      case None =>
        // We have no user with that email, send an email to the user to warn him
        mailer.send(Email(
          rq.messages("users.recover.email_title"),
          rq.messages("users.recover.email_from") + " <noreply@japan-impact.ch>",
          Seq(email),
          bodyText = Some(rq.messages("users.recover.no_user_email_text"))
        ))
    }
  }

  /**
    * Confirm the email of a user
    *
    * @param email the email to confirm
    * @param code  the confirmation code
    * @return A future, holding the confirmed user (if exists) or None if the email+code combination doesn't exist
    */
  def confirmEmail(email: String, code: String): Future[Option[RegisteredUser]] = {
    println("Confirming " + email + " with code " + code)


    getUser(email).flatMap({
      // Check if the user needs email confirmation and if the code is correct
      case Some(user@RegisteredUser(id, _, Some(c), _, _, _, _, _, _, _, _)) if c == code =>

        // Update the user to mark the email is confirmed, and return a new ticket
        val updated = user.copy(emailConfirmKey = None)
        updateUser(updated).map(_ => Some(updated))
      case _ =>
        Future(None)
    })
  }

  sealed trait LoginResult

  /**
    * The username or password is incorrect
    */
  case object BadLogin extends LoginResult

  /**
    * The email of the user is not confirmed
    */
  case object EmailNotConfirmed extends LoginResult

  /**
    * The login was successful.
    *
    * @param user the corresponding user
    */
  case class LoginSuccess(user: RegisteredUser) extends LoginResult

  def login(email: String, password: String): Future[LoginResult] = {
    getUser(email).flatMap {
      case Some(user@RegisteredUser(Some(id), _, emailConfirmKey, hash, algo, _, _, _, _, _, _)) =>
        // Check if password is correct
        if (hashes.check(algo, hash, password)) {

          // Check if email is confirmed
          if (emailConfirmKey.isEmpty) {

            // Try to upgrade password if needed
            val np = hashes.upgrade(algo, password)
            np match {
              case Some((newAlgo, newHash)) =>
                // The method returned a new (algo, pass) pair ==> we have to update!
                updateUser(user.copy(passwordAlgo = newAlgo, password = newHash))
              case _ => // do nothing
            }

            LoginSuccess(user)
          } else EmailNotConfirmed

        } else BadLogin


      case None =>
        // No account found... we just spend some time computing a fake password and return
        hashes fakeCheck password
        BadLogin
    }
  }

}
