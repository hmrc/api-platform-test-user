import sbt.Keys.baseDirectory
import sbt.Test
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning

lazy val appName = "api-platform-test-user"
lazy val appDependencies: Seq[ModuleID] = compile ++ test
lazy val akkaVersion = "2.5.23"
lazy val akkaHttpVersion = "10.0.15"
lazy val scope: String = "test, it"

lazy val compile = Seq(
  "uk.gov.hmrc" %% "bootstrap-play-26" % "1.4.0",
  "uk.gov.hmrc" %% "play-ui" % "8.8.0-play-26",
  "uk.gov.hmrc" %% "play-json-union-formatter" % "1.11.0",
  "uk.gov.hmrc" %% "domain" % "5.6.0-play-26",
  "uk.gov.hmrc" %% "mongo-lock" % "6.15.0-play-26",

  "org.mindrot" % "jbcrypt" % "0.4",

  "com.typesafe.play" %% "play-json" % "2.6.14",
  "com.typesafe.play" %% "play-json-joda" % "2.6.4",

  "com.typesafe.akka" %% "akka-stream"    % akkaVersion     force(),
  "com.typesafe.akka" %% "akka-protobuf"  % akkaVersion     force(),
  "com.typesafe.akka" %% "akka-slf4j"     % akkaVersion     force(),
  "com.typesafe.akka" %% "akka-actor"     % akkaVersion     force(),
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion force()
)

lazy val test = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % scope,
  "uk.gov.hmrc" %% "reactivemongo-test" % "4.15.0-play-26" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % scope,
  "org.pegdown" % "pegdown" % "1.6.0" % scope,
  "org.mockito" % "mockito-core" % "2.10.0" % scope,
  "org.scalaj" %% "scalaj-http" % "2.4.2" % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.21.0" % scope,
  "org.scalacheck" %% "scalacheck" % "1.13.5",
  "com.eclipsesource" %% "play-json-schema-validator" % "0.9.4" % scope
)

lazy val plugins: Seq[Plugins] = Seq.empty

lazy val playSettings: Seq[Setting[_]] = Seq(routesImport ++= Seq("uk.gov.hmrc.domain._", "uk.gov.hmrc.testuser.models._", "uk.gov.hmrc.testuser.Binders._"))

def emuellerBintrayResolver: MavenRepository = "emueller-bintray" at "https://dl.bintray.com/emueller/maven"

lazy val microservice = (project in file("."))
  .enablePlugins(Seq(_root_.play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins: _*)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    name := appName,
    targetJvm := "jvm-1.8",
    scalaVersion := "2.12.10",
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(warnScalaVersionEviction = false),
    resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo,
      emuellerBintrayResolver
    ),
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    majorVersion := 0
  )
  .configs(Test)
  .settings(
    Test / parallelExecution := false,
    Test / fork := false,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    Test / unmanagedResourceDirectories += baseDirectory.value / "test" / "resources"
  )
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory.value / "it",
    IntegrationTest / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    IntegrationTest / testGrouping := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    IntegrationTest / parallelExecution := false,
    addTestReportOption(IntegrationTest, "int-test-reports")
  )

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests map { test =>
    Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector("-Dtest.name=" + test.name))))
  }

// Coverage configuration
coverageMinimum := 93
coverageFailOnMinimum := true
coverageExcludedPackages :=
  "<empty>;" +
  "com.kenshoo.play.metrics.*;" +
  ".*definition.*;" +
  "prod.*;" +
  "testOnlyDoNotUseInAppConf.*;" +
  "app.*;" +
  "uk.gov.hmrc.BuildInfo;" +
  "uk.gov.hmrc.testuser.MicroserviceModule;" +
  "uk.gov.hmrc.testuser.controllers.javascript.*;" +
  "uk.gov.hmrc.testuser.controllers.Reverse.*;"
