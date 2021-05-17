package models

import java.net.URLEncoder
import java.sql.Timestamp
import anorm.SqlParser._
import anorm._
import ch.japanimpact.auth.api.{UserData, UserProfile}
import com.google.common.base.Preconditions
import data.{Address, AddressRowParser, RegisteredUser, RegisteredUserRowParser}

import javax.inject.Inject
import play.api.Configuration
import play.api.i18n.MessagesProvider
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc.MessagesRequestHeader
import services.{HashService, ReCaptchaService}
import utils.Implicits._
import utils.RandomUtils

import java.time.Instant
import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.util.Date
import scala.concurrent.{ExecutionContext, Future}

/**
 * @author zyuiop
 */
class UsersModel @Inject()(dbApi: play.api.db.DBApi, mailer: MailerClient, reCaptcha: ReCaptchaService, hashes: HashService)(implicit ec: ExecutionContext, config: Configuration) {
  private val db = dbApi database "default"

  /**
   * Gets a user in the database by its email
   *
   * @param email the email of the user to get
   * @return a future optional user (Some(user) if found, None if not)
   */
  def getUser(email: String): Future[Option[RegisteredUser]] = Future(db.withConnection { implicit c =>
    SQL"SELECT * FROM users WHERE email = $email".as(RegisteredUserRowParser.singleOpt)
  })

  def getUserProfile(id: Int): Future[Option[(RegisteredUser, Option[Address])]] = Future(db.withConnection { implicit c =>
    SQL"SELECT * FROM users LEFT JOIN users_addresses ua on users.id = ua.user_id WHERE id = $id"
      .as((RegisteredUserRowParser ~ AddressRowParser.?)
        .map { case a ~ b => (a, b) }
        .singleOpt)
  })

  def getUserProfiles(ids: Set[Int]): Future[Map[Int, (RegisteredUser, Option[Address])]] = Future(db.withConnection { implicit c =>
    SQL"SELECT * FROM users LEFT JOIN users_addresses ua on users.id = ua.user_id WHERE id IN ($ids)"
      .as((RegisteredUserRowParser ~ AddressRowParser.?)
        .map { case a ~ b => (a, b) }
        .*)
      .groupMapReduce(_._1.id.get)(pair => pair)((pair, _) => pair) // we know elements are unique
  })

  def getUsers: Future[Seq[UserProfile]] = Future(db.withConnection { implicit c =>
    SQL"SELECT * FROM users"
      .as(RegisteredUserRowParser.map(_.toUserProfile()).*)
  })

  def addAllowedScope(userId: Int, allowed: String) = Future(db.withConnection { implicit c =>
    SQL"INSERT IGNORE INTO users_allowed_scopes(user_id, scope) VALUES ($userId, $allowed)"
      .execute()
  })

  def removeAllowedScope(userId: Int, allowed: String) = Future(db.withConnection { implicit c =>
    SQL"DELETE FROM users_allowed_scopes WHERE user_id = $userId AND scope = $allowed"
      .execute()
  })

  /**
   * Gets a user in the database by its id
   *
   * @param id the id of the user to get
   * @return a future optional user (Some(user) if found, None if not)
   */
  def getUserById(id: Int): Future[Option[RegisteredUser]] = Future(db.withConnection { implicit c =>
    SQL"SELECT * FROM users WHERE id = $id".as(RegisteredUserRowParser.singleOpt)
  })

  def getUserData(id: Int): Future[Option[UserData]] =
    getUsersData(Set(id)).map(_.get(id))
  
  def getUsersData(ids: Set[Int]): Future[Map[Int, UserData]] = Future(db.withConnection { implicit c =>
    SQL"SELECT users.*, g.name, ua.*, uas.scope FROM users LEFT JOIN groups_members gm on users.id = gm.user_id LEFT JOIN `groups` g on gm.group_id = g.id LEFT JOIN users_addresses ua on users.id = ua.user_id LEFT JOIN users_allowed_scopes uas on users.id = uas.user_id WHERE users.id IN ($ids)"
      .as(((RegisteredUserRowParser ~ AddressRowParser.?) ~ (str("name").? ~ str("scope").?)).*)
      .map { case (user ~ address) ~ (name ~ scope) => ((user, address), (name, scope)) }
      .groupMap(_._1)(_._2)
      .map {
        case ((user, address), list) =>
          val (groups, scopes) = list.unzip

          user.id.get -> UserData(user.id, user.email, user.emailConfirmKey.isEmpty,
            user.toUserDetails, user.passwordAlgo, user.passwordReset.nonEmpty, user.passwordResetEnd,
            address.map(_.toUserAddress), groups = groups.flatten.toSet, scopes = scopes.flatten.toSet)
      }
  })

  /**
   * Create a user
   *
   * @param user the user to create
   * @return a future hodling the id of the inserted user
   */
  def createUser(user: RegisteredUser, address: Option[Address]): Future[Int] = Future(db.withConnection({ implicit c =>
    val uid = SqlUtils.insertOne("users", user)
    if (address.nonEmpty) SqlUtils.insertOne("users_addresses", address.get.copy(userId = uid))

    uid
  }))

  def setAddressAndPhone(addr: Address, phone: String): Future[Boolean] = Future(db.withConnection { implicit c =>
    SqlUtils.insertOne("users_addresses", addr, upsert = true) > 0 &&
      SQL"UPDATE users SET phone_number = $phone WHERE id = ${addr.userId}".executeUpdate() > 0
  })

  def getAllowedScopes(user: Int): Future[Set[String]] = Future(db.withConnection { implicit c =>
    SQL"SELECT scope FROM users_allowed_scopes WHERE user_id = $user UNION SELECT scope FROM groups_allowed_scopes JOIN `groups` g on groups_allowed_scopes.group_id = g.id JOIN groups_members gm on g.id = gm.group_id WHERE gm.user_id = $user"
      .as(SqlParser.str("scope").*)
      .toSet
  })

  /**
   * Updates a user whose id is set
   *
   * @param user the user to update/insert
   * @return the number of updated rows in a future
   */
  def updateUser(user: RegisteredUser): Future[Int] = {
    Preconditions.checkArgument(user.id.isDefined)
    Future(db.withConnection { implicit c => SqlUtils.replaceOne("users", user, "id") })
  }

  def update(id: Int, firstName: String, lastName: String, phone: String, newsletter: Boolean, addr: Address): Future[Boolean] = Future(
    db.withConnection { implicit c =>
      val ok = SQL"UPDATE users SET first_name = $firstName, last_name = $lastName, phone_number = $phone, newsletter = $newsletter WHERE id = $id".executeUpdate() > 0
      SqlUtils.insertOne("users_addresses", addr, upsert = true)

      ok
    }
  )

  def searchUsers(searchString: String): Future[Seq[RegisteredUser]] = {
    if (searchString.length < 3) Future.successful(Seq.empty)
    else
      Future(db.withConnection { implicit c =>
        SQL"""SELECT * FROM users WHERE email LIKE '$searchString%' OR CONCAT(first_name, ' ', last_name) LIKE '%$searchString%' LIMIT 10"""
          .as(RegisteredUserRowParser.*)
      })
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
            val confirmCode = sendConfirmEmail(emailConfirmBuilder)(user.email)

            val (algo, hash) = hashes.hash(user.password)

            createUser(
              user.copy(password = hash, passwordAlgo = algo, emailConfirmKey = Some(confirmCode), emailConfirmLastSent = Some(new Date())),
              address
            ).map(AccountCreated)
        }
      }
    )
  }

  sealed trait ResendConfirmEmailResult

  case object RetryLater extends ResendConfirmEmailResult
  case object NoAccountOrAlreadyConfirmed extends ResendConfirmEmailResult
  case object Success extends ResendConfirmEmailResult

  def resendConfirmEmail(email: String, emailConfirmBuilder: (String, String) => String)(implicit rq: MessagesRequestHeader): Future[ResendConfirmEmailResult] = {
    getUser(email).flatMap {
      case Some(c) if c.emailConfirmKey.nonEmpty =>
        val antiSpam = c.emailConfirmLastSent
          .map(_.toInstant)
          .map(_.plus(5, ChronoUnit.MINUTES))
          .exists(_.isAfter(Instant.now()))

        if (antiSpam) {
          Future successful RetryLater
        } else {
          updateUser(user = c.copy(emailConfirmLastSent = Some(new Date()))) map { _ =>
            sendConfirmEmail(emailConfirmBuilder)(c.email, c.emailConfirmKey.get)

            Success
          }
        }
      case _ => Future successful NoAccountOrAlreadyConfirmed
    }
  }

  private def sendConfirmEmail(emailConfirmBuilder: (String, String) => String)(email: String, confirmCode: String = RandomUtils.randomString(32))(implicit rq: MessagesRequestHeader) = {
    val emailEncoded = URLEncoder.encode(email, "UTF-8")

    val url = emailConfirmBuilder(emailEncoded, URLEncoder.encode(confirmCode, "UTF-8"))

    mailer.send(Email(
      rq.messages("users.register.email_title"),
      rq.messages("users.register.email_from") + " <noreply@japan-impact.ch>",
      Seq(email),
      bodyText = Some(rq.messages("users.register.email_text", url))
    ))

    confirmCode
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
      case Some(user) if user.emailConfirmKey.contains(code) =>

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
  case class EmailNotConfirmed(canResend: Boolean) extends LoginResult

  /**
   * The login was successful.
   *
   * @param user the corresponding user
   */
  case class LoginSuccess(user: RegisteredUser) extends LoginResult

  def login(email: String, password: String): Future[LoginResult] = {
    getUser(email).flatMap {
      case Some(user) =>
        // Check if password is correct
        if (hashes.check(user.passwordAlgo, user.password, password)) {

          // Check if email is confirmed
          if (user.emailConfirmKey.isEmpty) {

            // Try to upgrade password if needed
            val np = hashes.upgrade(user.passwordAlgo, password)
            np match {
              case Some((newAlgo, newHash)) =>
                // The method returned a new (algo, pass) pair ==> we have to update!
                updateUser(user.copy(passwordAlgo = newAlgo, password = newHash))
              case _ => // do nothing
            }

            LoginSuccess(user)
          } else EmailNotConfirmed(!user.emailConfirmLastSent.map(_.toInstant).map(_.plus(5, ChronoUnit.MINUTES)).exists(_.isAfter(Instant.now())))

        } else BadLogin

      case None =>
        // No account found... we just spend some time computing a fake password and return
        hashes fakeCheck password
        BadLogin
    }
  }

}
