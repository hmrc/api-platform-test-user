import scoverage.ScoverageKeys._

object ScoverageSettings {
  def apply() = Seq(
    coverageMinimum := 90,    //TODO - increase to 93 after investigating auth refresh
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageExcludedPackages :=  Seq(
      "uk.gov.hmrc.testuser.MicroserviceModule" +
      "<empty>",
      "prod.*",
      "testOnlyDoNotUseInAppConf.*",
      "app.*",
      ".*Reverse.*",
      ".*Routes.*",
      "com\\.kenshoo\\.play\\.metrics\\.*",
      "uk\\.gov\\.hmrc\\.testuser\\.MicroserviceModule",
      ".*definition.*",
      ".*BuildInfo.*",
      ".*javascript"
    ).mkString(";")
  )
}