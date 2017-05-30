import javax.inject._

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import play.api.{Configuration, Environment}
import v1.bob._

/**
  * Sets up custom components for Play.
  *
  * https://www.playframework.com/documentation/2.5.x/ScalaDependencyInjection
  */
class Module(environment: Environment, configuration: Configuration)
    extends AbstractModule
    with ScalaModule {

  override def configure() = {
    bind[BobRepository].to[BobRepositoryImpl].in[Singleton]
    bind[PollRepository].to[PollRepositoryImpl].in[Singleton]
    bind[CandidateRepository].to[CandidateRepositoryImpl].in[Singleton]
    bind[VoteRepository].to[VoteRepositoryImpl].in[Singleton]
  }
}
