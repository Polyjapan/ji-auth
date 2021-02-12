import java.time.Clock
import com.google.inject.{AbstractModule, Provides}
import com.yubico.webauthn.data.RelyingPartyIdentity
import play.api.Configuration

class Module extends AbstractModule {
  @Provides
  def clock: Clock = Clock.systemUTC()

  @Provides
  def rpIdentity(conf: Configuration): RelyingPartyIdentity =
    RelyingPartyIdentity.builder().id(conf.get[String]("webauthn.identification")).name(conf.get[String]("webauthn.name")).build()
}
