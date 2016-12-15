/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object MicroServiceBuild extends Build with MicroService {

  val appName = "api-platform-test-user"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val microserviceBootstrapVersion = "5.8.0"
  private val playAuthVersion = "4.2.0"
  private val playHealthVersion = "2.0.0"
  private val logbackJsonLoggerVersion = "3.1.0"
  private val playUrlBindersVersion = "2.0.0"
  private val playConfigVersion = "3.0.0"
  private val domainVersion = "4.0.0"
  private val hmrcReactiveMongoTestVersion = "1.6.0"
  private val hmrcTestVersion = "2.1.0"
  private val scalaTestVersion = "2.2.6"
  private val pegdownVersion = "1.6.0"
  private val scalaTestPlusVersion = "1.2.0"
  private val hmrcPlayJsonUnionFormatterVersion = "1.0.0"
  private val scalaCheckVersion = "1.12.6"
  private val mockitoVersion = "1.9.5"
  private val scalaJVersion = "1.1.5"
  private val jBcryptVersion = "0.3m"

  private val playReactivemongoVersion = "5.1.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo" % playReactivemongoVersion,

    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion,
    "uk.gov.hmrc" %% "play-authorisation" % playAuthVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-url-binders" % playUrlBindersVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc" %% "play-json-union-formatter" % hmrcPlayJsonUnionFormatterVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "org.scalacheck" %% "scalacheck" % scalaCheckVersion,
    "org.mindrot" % "jbcrypt" % jBcryptVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test, it"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "uk.gov.hmrc" %% "reactivemongo-test" % hmrcReactiveMongoTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus" %% "play" % scalaTestPlusVersion %  scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % mockitoVersion % scope,
      "org.scalaj" %% "scalaj-http" % scalaJVersion % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}
