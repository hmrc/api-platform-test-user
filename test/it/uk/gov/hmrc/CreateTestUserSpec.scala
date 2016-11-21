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

package it.uk.gov.hmrc

import java.util.concurrent.TimeUnit

import org.apache.http.HttpStatus.SC_CREATED
import org.scalatest._
import play.api.Mode
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.TestServer
import uk.gov.hmrc.mongo.MongoConnector
import uk.gov.hmrc.testuser.models.{TestOrganisation, TestOrganisationResponse, TestIndividual, TestIndividualResponse}
import uk.gov.hmrc.testuser.repository.TestUserMongoRepository
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.testuser.models.JsonFormatters._

import scala.concurrent.Await._
import scala.concurrent.duration.Duration
import scalaj.http.Http

class CreateTestUserSpec extends FeatureSpec with Matchers
with GivenWhenThen with BeforeAndAfterEach with BeforeAndAfterAll {

  val port = 9999
  val timeout = Duration(5, TimeUnit.SECONDS)
  val serviceUrl = s"http://localhost:$port"
  var server: TestServer = null

  feature("Create a test user") {

    scenario("Create an individual") {

      When("I request the creation of an individual")
      val response = Http(s"$serviceUrl/individual").postForm.asString

      Then("The response contains the details of the individual")
      response.code shouldBe SC_CREATED
      val individualResponse = Json.parse(response.body).as[TestIndividualResponse]

      And("The individual is stored in mongo")
      val individual = result(mongoRepository.fetchByUsername(individualResponse.username), timeout).get.asInstanceOf[TestIndividual]
      TestIndividualResponse.from(individual) shouldBe individualResponse
    }

    scenario("Create an organisation") {

      When("I request the creation of an organisation is received")
      val response = Http(s"$serviceUrl/organisation").postForm.asString

      Then("The response contains the details of the organisation")
      response.code shouldBe SC_CREATED
      val organisationResponse = Json.parse(response.body).as[TestOrganisationResponse]

      And("The organisation is stored in mongo")
      val organisation = result(mongoRepository.fetchByUsername(organisationResponse.username), timeout).get.asInstanceOf[TestOrganisation]
      TestOrganisationResponse.from(organisation) shouldBe organisationResponse
    }
  }

  override def beforeAll = {
    startServer()
    result(mongoRepository.drop, timeout)
    result(mongoRepository.ensureIndexes, timeout)
  }

  override def afterAll = {
    stopServer()
    result(mongoRepository.drop, timeout)
    result(mongoRepository.ensureIndexes, timeout)
  }

  private def startServer() = {
    val app = new  GuiceApplicationBuilder()
      .configure(Map("run.mode" -> "It", "mongodb.uri" -> "mongodb://localhost:27017/api-platform-test-user-it"))
      .in(Mode.Prod)
      .build()
    server = new TestServer(port, app)
    server.start()
  }

  def stopServer() = {
    server.stop()
  }

  def mongoRepository = {
    implicit val mongo = MongoConnector("mongodb://localhost:27017/api-platform-test-user-it").db
    new TestUserMongoRepository()
  }
}
