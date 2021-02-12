package models


import anorm.SqlParser._
import anorm._
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.common.cache.{Cache, CacheBuilder}
import com.yubico.webauthn.{AssertionRequest, CredentialRepository, FinishAssertionOptions, FinishRegistrationOptions, RegisteredCredential, RegistrationResult, RelyingParty, StartAssertionOptions, StartRegistrationOptions}
import com.yubico.webauthn.data.{AuthenticatorAssertionResponse, AuthenticatorAttestationResponse, ByteArray, ClientAssertionExtensionOutputs, ClientRegistrationExtensionOutputs, PublicKeyCredential, PublicKeyCredentialCreationOptions, PublicKeyCredentialDescriptor, RelyingPartyIdentity, UserIdentity}
import com.yubico.webauthn.exception.{AssertionFailedException, RegistrationFailedException}
import data.{RegisteredUser, RegisteredUserRowParser}
import play.api.libs.json.JsObject

import java.security.SecureRandom
import java.sql.Blob
import java.time.Duration
import java.util
import java.util.{Optional, UUID}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.jdk.DurationConverters._
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

class WebAuthnModel @Inject()(dbApi: play.api.db.DBApi, rpi: RelyingPartyIdentity, users: UsersModel)(implicit ec: ExecutionContext){
  private val db = dbApi database "default"

  private class Repo extends CredentialRepository {
    private object RegisteredCredentialParser extends RowParser[RegisteredCredential] {
      override def apply(v1: Row): SqlResult[RegisteredCredential] = {
        (str("key_uid") ~ str("user_handle") ~ str("key_cose")).apply(v1)
          .map {
            case uid ~ handle ~ cose => RegisteredCredential.builder()
              .credentialId(ByteArray.fromBase64(uid))
              .userHandle(ByteArray.fromBase64(handle))
              .publicKeyCose(ByteArray.fromBase64(cose))
              .build()
          }
      }
    }

    override def getCredentialIdsForUsername(username: String): util.Set[PublicKeyCredentialDescriptor] = {
      val userId = username.toIntOption

      userId.toSet // this set contains either 0 or 1 element, it's essentially an option
        .flatMap { id: Int =>
        db.withConnection { implicit c =>
          SQL"SELECT key_uid FROM webauthn_keys WHERE user_id = $id"
            .as(str("key_uid").*)
            .map(strKeyId => ByteArray.fromBase64(strKeyId))
            .map(keyId => PublicKeyCredentialDescriptor.builder().id(keyId).build())
        }.toSet
      }.asJava
    }

    override def getUserHandleForUsername(username: String): Optional[ByteArray] = {
      val userId = username.toIntOption

      userId.flatMap { id =>
        db.withConnection { implicit c =>
          SQL"SELECT user_handle FROM users WHERE id = $id"
            .as(str("user_handle").singleOpt)
            .map(handle => ByteArray.fromBase64(handle))
        }
      }.toJava
    }

    override def getUsernameForUserHandle(userHandle: ByteArray): Optional[String] = {
      db.withConnection { implicit c =>
        SQL"SELECT id FROM users WHERE user_handle = ${userHandle.getBase64}"
          .as(int("id").singleOpt)
          .map(_.toString)
      }.toJava
    }

    override def lookup(credentialId: ByteArray, userHandle: ByteArray): Optional[RegisteredCredential] = {
      println(s"Looking up key with user_handle = ${userHandle.getBase64} AND key_uid = ${credentialId.getBase64}")

      val opt = db.withConnection { implicit c =>
        SQL"SELECT * FROM webauthn_keys WHERE user_handle = ${userHandle.getBase64} AND key_uid = ${credentialId.getBase64}"
          .as(RegisteredCredentialParser.singleOpt)
      }

      opt.toJava
    }

    override def lookupAll(credentialId: ByteArray): util.Set[RegisteredCredential] = {
      db.withConnection { implicit c =>
        SQL"SELECT * FROM webauthn_keys WHERE key_uid = ${credentialId.getBase64}"
          .as(RegisteredCredentialParser.*)
      }.toSet.asJava
    }
  }

  private val random = new SecureRandom
  private val rp = RelyingParty.builder()
    .identity(rpi)
    .credentialRepository(new Repo)
    .allowOriginPort(true)
    .build()
  private val registrationRequests: Cache[UUID, (Int, PublicKeyCredentialCreationOptions)] =
    CacheBuilder.newBuilder()
      .expireAfterWrite(5.minutes.toJava)
      .build()
  private val authRequests: Cache[UUID, (Int, AssertionRequest)] =
    CacheBuilder.newBuilder()
      .expireAfterWrite(5.minutes.toJava)
      .build()

  // JSON Object Mapping for Java (doesn't integrate natively with Play's nice API sadly)
  private val jsonMapper: ObjectMapper = new ObjectMapper()
    .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
    .registerModule(new Jdk8Module())

  /**
   * Starts a registration request
   * @param user the user for which you want to enable webauthn
   * @return a pair containing the Json object to send to the client and the UUID of the request (to send to the client as well)
   */
  def startRegistration(user: RegisteredUser): Future[(String, UUID)] = {
    Future {
      val handle: ByteArray = if (user.handle.isEmpty) {
        val handleBytes = Array.ofDim[Byte](64)
        random.nextBytes(handleBytes)

        val handle = new ByteArray(handleBytes)
        // Update the user in the database with the new handle
        users.updateUser(user.copy(userHandle = Some(handle.getBase64)))

        handle
      } else user.handle.get

      val request = rp.startRegistration(StartRegistrationOptions.builder().user(
        UserIdentity.builder()
          .name(user.id.get.toString)
          .displayName(user.firstName + " " + user.lastName)
          .id(handle)
          .build()
      )
        .timeout(60 * 4 * 1000L) // 4mn timeout on the client
        .build())

      val id = UUID.randomUUID()

      registrationRequests.put(id, (user.id.get, request))

      (jsonMapper.writeValueAsString(request), id)
    }
  }

  private def insertKey(user: RegisteredUser, keyName: String, keyData: RegistrationResult) = {
    db.withConnection { implicit c =>
      SQL"INSERT INTO webauthn_keys(user_id, user_handle, key_name, key_uid, key_cose) VALUES (${user.id.get}, ${user.userHandle.get}, $keyName, ${keyData.getKeyId.getId.getBase64}, ${keyData.getPublicKeyCose.getBase64})"
        .execute()
    }
  }

  /**
   * Complete the registration of a webauthn key
   * @param user the user to register
   * @param requestId the UUID of the request
   * @param response the response sent by the user
   * @param name the name to give to this webauthn key for the client
   * @return a future with true if the registration succeeded, false if not
   */
  def finishRegistration(user: RegisteredUser,
                         requestId: UUID,
                         responseJson: JsObject,
                         name: String): Future[Boolean] = {

    val response = PublicKeyCredential.parseRegistrationResponseJson(responseJson.toString())

    Future {
      val request = Option(registrationRequests.getIfPresent(requestId))
        .flatMap {
          case (id, options) if id == user.id.get => Some(options)
          case _ =>
            println("No request ID")
            None
        }

      if (request.isDefined) {
        try {
          val result = rp.finishRegistration(FinishRegistrationOptions.builder()
            .request(request.get)
            .response(response)
            .build())

          insertKey(user, name, result)

          true
        } catch {
          case ex: RegistrationFailedException =>
            ex.printStackTrace()
            false
        }
      } else {
        println("Request not found")
        false
      }
    }
  }

  def getKeysForUser(user: Int) = Future { db.withConnection { implicit c =>

  }}

  def userHasKeys(user: Int): Future[Boolean] = Future { db.withConnection { implicit c =>
    SQL"SELECT * FROM webauthn_keys WHERE user_id = $user LIMIT 1".as(int("user_id").singleOpt)
      .exists(_ == user)
  }}

  /**
   * Start authenticating a user
   * @param user the user to authenticate
   * @return a future with the json to pass to the JS api + the UUID of the auth request
   */
  def startAuthentication(user: RegisteredUser): Future[(String, UUID)] = {
    Future {
      val rq = rp.startAssertion(StartAssertionOptions.builder()
        .username(user.id.get.toString)
        .build())
      val id = UUID.randomUUID()

      authRequests.put(id, (user.id.get, rq))

      (jsonMapper.writeValueAsString(rq), id)
    }
  }

  /**
   * Complete a user authentication
   * @param user
   * @param requestId
   * @param response
   * @return
   */
  def validateAuthentication(user: RegisteredUser,
                             requestId: UUID,
                             responseJson: JsObject): Future[Boolean] = {

    val response = PublicKeyCredential.parseAssertionResponseJson(responseJson.toString())
    println(response)
    println(response.getResponse.getUserHandle)

    Future {
      val request = Option(authRequests.getIfPresent(requestId))
        .flatMap {
          case (id, options) if id == user.id.get =>
            Some(options)
          case _ =>
            println("WebAuthN: user " + user.id.get + " attempted to use non-existing or incorrect request id " + requestId)
            None
        }

      if (request.isDefined) {
        try {
          val result = rp.finishAssertion(FinishAssertionOptions.builder
            .request(request.get)
            .response(response)
            .build)

          println(result)

          if (result.isSuccess) result.getUserHandle.equals(user.handle.get)
          else false
        } catch {
          case ex: AssertionFailedException =>
            ex.printStackTrace()
            false
        }
      } else {
        false
      }
    }
  }

}
