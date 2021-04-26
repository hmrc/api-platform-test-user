import sbt._

object AppDependencies {
  def apply(): Seq[ModuleID] = compile ++ test
  
  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-26" % "4.2.0",
    "uk.gov.hmrc" %% "play-ui"                   % "8.8.0-play-26",
    "uk.gov.hmrc" %% "play-json-union-formatter" % "1.11.0",
    "uk.gov.hmrc" %% "domain"                    % "5.6.0-play-26",
    "uk.gov.hmrc" %% "mongo-lock"                % "6.23.0-play-26",
    "org.mindrot" % "jbcrypt"                    % "0.4",

    "com.typesafe.play" %% "play-json"           % "2.8.1",
    "com.typesafe.play" %% "play-json-joda"      % "2.8.1",
    "org.scalacheck"    %% "scalacheck"          % "1.13.5"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "hmrctest"           % "3.9.0-play-26",
    "uk.gov.hmrc"            %% "reactivemongo-test" % "4.21.0-play-26",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3",
    "org.pegdown"            % "pegdown"             % "1.6.0",
    "org.mockito"            % "mockito-core"        % "2.10.0",
    "org.scalaj"             %% "scalaj-http"        % "2.4.2",
    "com.github.tomakehurst" % "wiremock-jre8"       % "2.21.0"
  ).map (m => m % "test,it")
}
