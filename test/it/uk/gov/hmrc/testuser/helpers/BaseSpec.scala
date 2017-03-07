/*
 * Copyright 2017 HM Revenue & Customs
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

package it.uk.gov.hmrc.testuser.helpers

import java.util.concurrent.TimeUnit

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import it.uk.gov.hmrc.testuser.helpers.stubs.AuthLoginApiStub
import org.scalatest._
import org.scalatestplus.play.OneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.MongoConnector
import uk.gov.hmrc.testuser.repository.TestUserMongoRepository
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Await._
import scala.concurrent.duration.Duration

trait BaseSpec extends FeatureSpec with BeforeAndAfterAll with BeforeAndAfterEach with Matchers with OneServerPerSuite
with GivenWhenThen {

  override lazy val port = 9000
  implicit override lazy val app: Application = GuiceApplicationBuilder().configure(
      "auditing.enabled" -> false,
      "auditing.traceRequests" -> false,
      "microservice.services.auth-login-api.port" -> AuthLoginApiStub.port,
      "mongodb.uri" -> "mongodb://localhost:27017/api-platform-test-user-it",
      "run.mode" -> "It"
    ).build()

  val timeout = Duration(5, TimeUnit.SECONDS)
  val serviceUrl = s"http://localhost:$port"
  val mocks = Seq(AuthLoginApiStub)

  override protected def beforeEach(): Unit = {
    mocks.foreach(m => if (!m.server.isRunning) m.server.start())
    result(mongoRepository.drop, timeout)
    result(mongoRepository.ensureIndexes, timeout)

  }

  override protected def afterEach(): Unit = {
    mocks.foreach(_.mock.resetMappings())
  }

  override protected def afterAll(): Unit = {
    mocks.foreach(_.server.stop())
    result(mongoRepository.drop, timeout)
  }

  def mongoRepository = {
    implicit val mongo = MongoConnector("mongodb://localhost:27017/api-platform-test-user-it").db
    new TestUserMongoRepository()
  }
}

case class MockHost(port: Int) {
  val server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))
  val mock = new WireMock("localhost", port)
  val url = s"http://localhost:$port"
}
