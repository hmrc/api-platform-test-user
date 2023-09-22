import sbt.Keys.baseDirectory
import sbt.Test
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import bloop.integrations.sbt.BloopDefaults

lazy val appName = "api-platform-test-user"

lazy val playSettings: Seq[Setting[_]] = Seq(routesImport ++= Seq("uk.gov.hmrc.domain._", "uk.gov.hmrc.testuser.models._", "uk.gov.hmrc.testuser.Binders._"))

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(ScoverageSettings())
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    name := appName,
    scalaVersion := "2.13.8",
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    majorVersion := 0,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .configs(Test)
  .settings(
    inConfig(Test)(BloopDefaults.configSettings),
    Test / parallelExecution := false,
    Test / fork := false,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    Test / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    Test / unmanagedSourceDirectories += baseDirectory.value / "test",
    Test / unmanagedResourceDirectories += baseDirectory.value / "test" / "resources"
  )
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    inConfig(IntegrationTest)(BloopDefaults.configSettings),
    DefaultBuildSettings.integrationTestSettings(),
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    IntegrationTest / unmanagedSourceDirectories += baseDirectory.value / "it",
    IntegrationTest / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    IntegrationTest / testGrouping := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    IntegrationTest / parallelExecution := false,
    addTestReportOption(IntegrationTest, "int-test-reports")
  )
  .settings(
    scalacOptions ++= Seq(
    "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
    "-Wconf:cat=unused&src=.*Routes\\.scala:s",
    "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s"
    )
  )

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests map { test =>
    Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector("-Dtest.name=" + test.name))))
  }
