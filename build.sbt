import _root_.play.sbt.routes.RoutesKeys.routesGenerator
import play.core.PlayVersion
import play.routes.compiler.StaticRoutesGenerator
import play.sbt.PlayImport._
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.{SbtAutoBuildPlugin, _}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning

lazy val appName = "api-platform-test-user"
lazy val appDependencies: Seq[ModuleID] = compile ++ test

lazy val compile = Seq(
  "uk.gov.hmrc" %% "play-reactivemongo" % "5.2.0",
  ws,
  "uk.gov.hmrc" %% "microservice-bootstrap" % "6.11.0",
  "uk.gov.hmrc" %% "play-url-binders" % "2.1.0",
  "uk.gov.hmrc" %% "play-hmrc-api" % "2.0.0",
  "uk.gov.hmrc" %% "play-json-union-formatter" % "1.0.0",
  "uk.gov.hmrc" %% "domain" % "5.0.0",
  "uk.gov.hmrc" %% "mongo-lock" % "4.1.0",
  "org.scalacheck" %% "scalacheck" % "1.12.6",
  "org.mindrot" % "jbcrypt" % "0.4"
)

lazy val scope: String = "test, it"

lazy val test = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "2.3.0" % scope,
  "uk.gov.hmrc" %% "reactivemongo-test" % "2.0.0" % scope,
  "org.scalatest" %% "scalatest" % "3.0.4" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
  "org.pegdown" % "pegdown" % "1.6.0" % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "org.mockito" % "mockito-core" % "1.10.19" % scope,
  "org.scalaj" %% "scalaj-http" % "1.1.6" % scope,
  "com.github.tomakehurst" % "wiremock" % "1.58" % "test,it"
)

lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings : Seq[Setting[_]] = Seq(routesImport ++= Seq("uk.gov.hmrc.domain._", "uk.gov.hmrc.testuser.models._", "uk.gov.hmrc.testuser.Binders._"))

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

// Coverage configuration
coverageMinimum := 95
coverageFailOnMinimum := true
coverageExcludedPackages := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;testOnlyDoNotUseInAppConf.*;app.*;uk.gov.hmrc.BuildInfo"
