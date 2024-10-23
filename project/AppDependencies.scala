import sbt._
import play.core.PlayVersion

object AppDependencies {
  def apply(): Seq[ModuleID] = compile ++ test

  lazy val bootstrapVersion    = "9.5.0"
  lazy val mongoVersion        = "2.2.0"
  lazy val commonDomainVersion = "0.17.0"

  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc"       %% "domain-play-30"             % "10.0.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % mongoVersion,
    "org.mindrot"        % "jbcrypt"                    % "0.4",
    "uk.gov.hmrc"       %% "api-platform-common-domain" % commonDomainVersion,
    "org.scalacheck"    %% "scalacheck"                 % "1.17.1"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-test-play-30"               % bootstrapVersion,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-test-play-30"              % mongoVersion,
    "uk.gov.hmrc"                   %% "api-platform-common-domain-fixtures"  % commonDomainVersion,
    "org.scalatestplus"             %% "scalacheck-1-15"                      % "3.2.11.0",
    "org.pegdown"                    % "pegdown"                              % "1.6.0",
    "com.softwaremill.sttp.client3" %% "core"                                 % "3.9.8"

  ).map(m => m % "test")
}
