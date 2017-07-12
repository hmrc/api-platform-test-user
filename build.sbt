import play.sbt.PlayImport._
import play.core.PlayVersion
import sbt.Tests.{SubProcess, Group}
import play.routes.compiler.StaticRoutesGenerator
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc._
import DefaultBuildSettings._
import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt, SbtAutoBuildPlugin}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import _root_.play.sbt.routes.RoutesKeys.routesGenerator

lazy val appName = "api-platform-test-user"
lazy val appDependencies: Seq[ModuleID] = compile ++ test

lazy val microserviceBootstrapVersion = "5.13.0"
lazy val playAuthVersion = "4.3.0"
lazy val playHealthVersion = "2.1.0"
lazy val logbackJsonLoggerVersion = "3.1.0"
lazy val playUrlBindersVersion = "2.1.0"
lazy val playConfigVersion = "4.3.0"
lazy val domainVersion = "4.1.0"
lazy val mongoLockVersion = "4.1.0"
lazy val hmrcReactiveMongoTestVersion = "2.0.0"
lazy val hmrcTestVersion = "2.3.0"
lazy val scalaTestVersion = "2.2.6"
lazy val pegdownVersion = "1.6.0"
lazy val scalaTestPlusVersion = "1.5.1"
lazy val hmrcPlayJsonUnionFormatterVersion = "1.0.0"
lazy val scalaCheckVersion = "1.12.6"
lazy val mockitoVersion = "1.10.19"
lazy val scalaJVersion = "1.1.6"
lazy val jBcryptVersion = "0.4"

lazy val playReactivemongoVersion = "5.2.0"

lazy val compile = Seq(
  "uk.gov.hmrc" %% "play-reactivemongo" % playReactivemongoVersion,
  ws,
  "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion,
  "uk.gov.hmrc" %% "play-authorisation" % playAuthVersion,
  "uk.gov.hmrc" %% "play-health" % playHealthVersion,
  "uk.gov.hmrc" %% "play-url-binders" % playUrlBindersVersion,
  "uk.gov.hmrc" %% "play-config" % playConfigVersion,
  "uk.gov.hmrc" %% "play-hmrc-api" % "1.4.0",
  "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLoggerVersion,
  "uk.gov.hmrc" %% "play-json-union-formatter" % hmrcPlayJsonUnionFormatterVersion,
  "uk.gov.hmrc" %% "domain" % domainVersion,
  "uk.gov.hmrc" %% "mongo-lock" % mongoLockVersion,
  "org.scalacheck" %% "scalacheck" % scalaCheckVersion,
  "org.mindrot" % "jbcrypt" % jBcryptVersion
)

lazy val scope: String = "test, it"

lazy val test = Seq(
  "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
  "uk.gov.hmrc" %% "reactivemongo-test" % hmrcReactiveMongoTestVersion % scope,
  "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
  "org.pegdown" % "pegdown" % pegdownVersion % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "org.mockito" % "mockito-core" % mockitoVersion % scope,
  "org.scalaj" %% "scalaj-http" % scalaJVersion % scope,
  "com.github.tomakehurst" % "wiremock" % "1.58" % "test,it"
)

lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty

def unitFilter(name: String): Boolean = name startsWith "unit"
def itTestFilter(name: String): Boolean = name startsWith "it"

lazy val microservice = (project in file("."))
  .enablePlugins(Seq(_root_.play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin) ++ plugins: _*)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    name := appName,
    scalaVersion := "2.11.11",
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    parallelExecution in Test := false,
    fork in Test := false,
    testOptions in Test := Seq(Tests.Filter(unitFilter)),
    routesGenerator := StaticRoutesGenerator
  )
  .settings(unmanagedResourceDirectories in Compile += baseDirectory.value / "resources")
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    testOptions in IntegrationTest := Seq(Tests.Filter(itTestFilter)),
    unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest) (base => Seq(base / "test")),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false)
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests map {
    test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }

