import sbt._
import play.core.PlayVersion

object AppDependencies {
  def apply(): Seq[ModuleID] = compile ++ test
  
  val AkkaVersion = "2.6.19"
  
  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.25.0",
    "uk.gov.hmrc"             %% "play-json-union-formatter"  % "1.15.0-play-28",
    "uk.gov.hmrc"             %% "domain"                     % "6.2.0-play-28",
    "uk.gov.hmrc"             %% "mongo-lock"                 % "7.0.0-play-28",
    "org.mindrot"             % "jbcrypt"                     % "0.4",
    "com.typesafe.play"       %% "play-json"                  % "2.9.2",
    "org.scalacheck"          %% "scalacheck"                 % "1.13.5"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "5.25.0",
    "com.typesafe.play"       %% "play-test"                  % PlayVersion.current,
    "com.typesafe.akka"       %% "akka-stream-testkit"        % AkkaVersion,
    "uk.gov.hmrc"             %% "reactivemongo-test"         % "5.0.0-play-28",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.17.12",
    "org.scalatestplus"       %% "scalacheck-1-15"            % "3.2.10.0",
    "org.pegdown"             %  "pegdown"                    % "1.6.0",
    "org.scalaj"              %% "scalaj-http"                % "2.4.2",
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone"   % "2.27.2"
  ).map (m => m % "test,it")
}
