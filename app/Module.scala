import java.time.Clock
import com.google.inject.{AbstractModule, Provides}
import com.yubico.webauthn.data.RelyingPartyIdentity
import play.api.Configuration

class Module extends AbstractModule {
  @Provides
  def clock: Clock = Clock.systemUTC()

  @Provides @Singleton
  def rpIdentity(conf: Configuration): RelyingPartyIdentity =
    RelyingPartyIdentity.builder().id(conf.get("webauthn.identification")).name(conf.get("webauthn.name")).build()
}
