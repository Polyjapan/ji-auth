import java.time.Clock

import com.google.inject.{AbstractModule, Provides}

class Module extends AbstractModule {
  @Provides
  def clock: Clock = Clock.systemUTC()

}
