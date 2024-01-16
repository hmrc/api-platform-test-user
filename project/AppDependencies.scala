import sbt._
import play.core.PlayVersion

object AppDependencies {
  def apply(): Seq[ModuleID] = compile ++ test

  lazy val bootstrapVersion = "8.4.0"
  lazy val mongoVersion     = "1.7.0"

  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-json-union-formatter"  % "1.20.0",
    "uk.gov.hmrc"       %% "domain-play-30"             % "9.0.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % mongoVersion,
    "org.mindrot"        % "jbcrypt"                    % "0.4",
    "uk.gov.hmrc"       %% "api-platform-common-domain" % "0.10.0",
    "org.scalacheck"    %% "scalacheck"                 % "1.14.1"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"           %% "bootstrap-test-play-30"           % bootstrapVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-test-play-30"          % mongoVersion,
    "uk.gov.hmrc"           %% "api-platform-test-common-domain"  % "0.10.0",
    "org.scalatestplus"     %% "scalacheck-1-15"                  % "3.2.10.0",
    "org.pegdown"            % "pegdown"                          % "1.6.0",
    "org.scalaj"            %% "scalaj-http"                      % "2.4.2",
  ).map(m => m % "test,it")
}
