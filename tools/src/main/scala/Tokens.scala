import java.io.{FileReader, PrintWriter}
import java.nio.file.{Files, Path}
import java.security.spec.{ECGenParameterSpec, PKCS8EncodedKeySpec}
import java.security._
import java.time.Clock

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.{PEMKeyPair, PEMParser, PKCS8Generator}
import org.bouncycastle.openssl.jcajce.{JcaPEMKeyConverter, JcaPEMWriter}
import pdi.jwt.{JwtAlgorithm, JwtBase64, JwtClaim, JwtJson}
import play.api.libs.json.Json

import scala.concurrent.duration.{Duration, _}
import scala.io.{Source, StdIn}
import scala.util.chaining._

object Tokens {
  private def parseArgs(args: List[String], map: Map[String, String] = Map()): Map[String, String] = args match {
    case "--private" :: value :: tail => parseArgs(tail, map.updated("privatekey", value))
    case "--public" :: value :: tail => parseArgs(tail, map.updated("publickey", value))
    case "--about" :: value :: tail => parseArgs(tail, map.updated("principal", value))
    // case "--by" :: value :: tail => parseArgs(tail, map.updated("issuer", value))
    case "--to" :: value :: tail => parseArgs(tail, map.updated("services", value))
    case "--duration" :: value :: tail => parseArgs(tail, map.updated("duration", value))
    case "--scopes" :: value :: tail => parseArgs(tail, map.updated("scopes", value))
    case "--keygen" :: tail => parseArgs(tail, map.updated("keygen", "true"))
    case "-v" :: tail => parseArgs(tail, map.updated("verbose", "true"))
    case Nil => map
  }

  implicit val clock: Clock = Clock.systemUTC()

  private def parseKey(key: String): Array[Byte] = JwtBase64.decodeNonSafe(
    key.replaceAll("-----BEGIN (.*)-----", "")
      .replaceAll("-----END (.*)-----", "")
      .replaceAll("\r\n", "")
      .replaceAll("\n", "")
      .trim
  )

  private def parsePrivateKey(file: String): PrivateKey = {
    val reader = new PEMParser(new FileReader(file))
    val keyInfo = reader.readObject().asInstanceOf[PEMKeyPair]
    new JcaPEMKeyConverter().setProvider("BC").getKeyPair(keyInfo).getPrivate
  }

  private def saveKey[T <: Key](key: T, file: String): T = {
    val gen = new JcaPEMWriter(new PrintWriter(file))
    gen.writeObject(key)
    gen.close()

    key
  }

  private def generateKeyPair(publicPath: String, privatePath: String): PrivateKey = {
    val ecGenSpec = new ECGenParameterSpec("P-521")
    val generatorEC = KeyPairGenerator.getInstance("ECDSA", "BC")
    generatorEC.initialize(ecGenSpec, new SecureRandom())

    val ecKey = generatorEC.generateKeyPair()

    // Save the keys
    saveKey(ecKey.getPublic, publicPath)
    saveKey(ecKey.getPrivate, privatePath)
  }

  private def readWithDefault(prompt: String, default: String) = {
    val v = StdIn.readLine(s"$prompt [$default]:")

    if (v.isBlank) default else v
  }

  def main(_args: Array[String]): Unit = {
    // Load BouncyCastle
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider())
    }

    // Generate or load keys
    val args = parseArgs(_args.toList)
    val verbose = args.contains("verbose")

    val privateKeyPath = args.getOrElse("privatekey", readWithDefault("Private key path:", "private.pem"))

    val pk = if (args.contains("kewgen") || !Files.exists(Path.of(privateKeyPath))) {
      // Generate the key
      if (verbose) println(s"Generating private key to $privateKeyPath (change with --private <path>)")
      val publicKeyPath = args.getOrElse("publickey", readWithDefault("Public key path:", "public.pem"))
      if (verbose) println(s"Generating public key to $privateKeyPath (change with --public <path>)")

      generateKeyPair(publicKeyPath, privateKeyPath).tap(_ => println("Generated keypair!"))
    } else {
      if (verbose) println(s"Loading private key from $privateKeyPath (change with --private <path>)")

      parsePrivateKey(privateKeyPath)
    }

    // Generate token
    /*

    case "--about" :: value :: tail => parseArgs(tail, map.updated("principal", value))
    case "--to" :: value :: tail => parseArgs(tail, map.updated("services", value))
    case "--scopes" :: value :: tail => parseArgs(tail, map.updated("scopes", value))
     */
    val about = args.getOrElse("principal", StdIn.readLine("Subject (principal) (who does the token represent - 'sub' field):"))
    val to = args.getOrElse("services", StdIn.readLine("Targetted services, comma separated ('aud' field) (ex: uploads,auth):")).split(",").toSet
    val scopes = args.getOrElse("scopes", StdIn.readLine("Scopes, comma separated (ex: uploads/*,auth/*)")).split(",").toSet
    val duration = Duration(args.getOrElse("duration", readWithDefault("Validity duration (i.e. 7hours or 1day):", "12 hours")))

    val (period, token) = issueApiToken(about, scopes, to, duration)(pk)

    println()
    println(Console.GREEN + "Generated token!")
    println()
    println(Console.YELLOW + token)
    println()
    println(Console.YELLOW + s"The key is valid for $period seconds")
  }

  def issueApiToken(principal: String, scopes: Set[String], services: Set[String], duration: Duration)(key: PrivateKey): (Long, String) = {
    val time = if (duration.toHours > 48) 48.hours else duration

    val claim = JwtClaim(Json.obj(
      "scopes" -> scopes
    ).toString())
      .about(principal)
      .by("auth")
      .to(services)
      .issuedNow
      .expiresIn(time.toSeconds)


    (time.toSeconds, JwtJson.encode(claim, key, JwtAlgorithm.ES256))
  }

}
