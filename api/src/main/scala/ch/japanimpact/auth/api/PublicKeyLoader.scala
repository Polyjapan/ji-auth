package ch.japanimpact.auth.api

import javax.inject.{Inject, Singleton}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.io.Source


@Singleton
class PublicKeyLoader @Inject()(implicit executionContext: ExecutionContext, config: Configuration) {

  private lazy val pubKeyPath: String = config.get[String]("jwt.publicKeyPath")

  lazy val publicKey: String = {
    val src = if (pubKeyPath.startsWith("https://")) {
      Source.fromURL(pubKeyPath)
    } else {
      Source.fromFile(pubKeyPath)
    }

    val k = src.mkString
    src.close()
    k
  }
}