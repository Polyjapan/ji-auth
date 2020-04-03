package ch.japanimpact.auth.api.cas

import javax.inject.Inject
import play.api.Configuration

/**
  * Should be added to your configuration!
  * @param host (cas.hostname) - default auth.japan-impact.ch: the hostname (and port) of the CAS server
  * @param url (cas.uri) - default /cas/v2 - the base path of CAS on the given hostname
  * @param service (cas.service) - the domain of this service on CAS
  * @param protocol (cas.secure) - default true - set to false to use http instead of https
  */
case class CasConfiguration(host: String, url: String, service: String, protocol: String = "https") {
  def path(endpoint: String) =
    s"$protocol://$host/$url/$endpoint"
}

object CasConfiguration {
  @Inject()
  def apply(config: Configuration): CasConfiguration = {

    val hostname = config.getOptional[String]("cas.hostname").getOrElse("auth.japan-impact.ch")
    val url = config.getOptional[String]("cas.uri").getOrElse("/cas/v2")
    val service = config.get[String]("cas.service")
    val protocol = if (config.getOptional[Boolean]("cas.secure").getOrElse(true)) "https" else "http"

    CasConfiguration(hostname, url, service, protocol)
  }
}
