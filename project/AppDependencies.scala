import sbt._
import play.core.PlayVersion

object AppDependencies {
  def apply(): Seq[ModuleID] = compile ++ test
  
  val AkkaVersion = "2.6.20"
  lazy val bootstrapVersion = "7.12.0"
  lazy val mongoVersion = "0.74.0"

  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-json-union-formatter"  % "1.17.0-play-28",
    "uk.gov.hmrc"             %% "domain"                     % "6.2.0-play-28",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % mongoVersion,
    "org.mindrot"             % "jbcrypt"                     % "0.4",
    "com.typesafe.play"       %% "play-json"                  % "2.9.2",
    "org.scalacheck"          %% "scalacheck"                 % "1.13.5"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % mongoVersion,
    "com.typesafe.play"       %% "play-test"                  % PlayVersion.current,
    "com.typesafe.akka"       %% "akka-stream-testkit"        % AkkaVersion,
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.17.12",
    "org.scalatestplus"       %% "scalacheck-1-15"            % "3.2.10.0",
    "org.pegdown"             %  "pegdown"                    % "1.6.0",
    "org.scalaj"              %% "scalaj-http"                % "2.4.2",
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone"   % "2.27.2"
  ).map (m => m % "test,it")
}
