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

import org.apache.http.HttpStatus._
import org.scalatest._
import play.api.Mode
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.TestServer
import uk.gov.hmrc.mongo.MongoConnector
import uk.gov.hmrc.testuser.repository.TestUserMongoRepository

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.testuser.models.JsonFormatters._

import scala.concurrent.Await._
import scala.concurrent.duration.Duration
import scalaj.http.{Http, HttpResponse}
import org.mindrot.jbcrypt.{BCrypt => BCryptUtils}
import uk.gov.hmrc.testuser.models._

class TestUserSpec extends FeatureSpec with Matchers
with GivenWhenThen with BeforeAndAfterEach with BeforeAndAfterAll {

  val port = 9999
  val timeout = Duration(5, TimeUnit.SECONDS)
  val serviceUrl = s"http://localhost:$port"
  var server: TestServer = _

  private def authenticate(usr: String, pwd: String): HttpResponse[String] = {
    val payload = Json.parse(s"""{ "username": "$usr", "password" :"$pwd" }""").toString

    Http(s"$serviceUrl/authenticate")
      .headers("Content-type" -> "application/json")
      .postData(payload).asString
  }

  feature("Create a test user") {

    scenario("Create an individual") {

      When("I request the creation of an individual")
      val createdResponse = Http(s"$serviceUrl/individual").postForm.asString

      Then("The response contains the details of the individual created")
      createdResponse.code shouldBe SC_CREATED
      val individualCreated = Json.parse(createdResponse.body).as[CreateTestIndividualResponse]

      And("The individual is stored in Mongo with hashed password")
      val individualFromMongo = result(mongoRepository.fetchByUsername(individualCreated.username), timeout).get.asInstanceOf[TestIndividual]
      val expectedIndividualCreated = CreateTestIndividualResponse.from(individualFromMongo.copy(password = individualCreated.password))
      individualCreated shouldBe expectedIndividualCreated
      validatePassword(individualCreated.password, individualFromMongo.password) shouldBe true

      And("If I login with the individual's credentials")
      val loginIndividualResponse = authenticate(individualCreated.username, individualCreated.password)

      Then("The response contains the details of the individual created previously")
      loginIndividualResponse.code shouldBe SC_OK
      val individual = Json.parse(loginIndividualResponse.body).as[TestIndividualResponse]
      val expectedIndividual = TestIndividualResponse.from(individualFromMongo)
      individual shouldBe expectedIndividual

      Then("The response does not contain the password")
      loginIndividualResponse.body.toLowerCase shouldNot include("password")

      Then("The response contains 'individual' as user type")
      loginIndividualResponse.body.toLowerCase should include("\"usertype\":\"individual\"")


      val wrongUsername = "WrongUsername"
      val wrongPassword = "WrongPassword"

      And("If I login with an existing username")
      val wrongUsernameLoginResponse = authenticate(wrongUsername, individualCreated.password)

      Then("The response says that the username does not exist")
      wrongUsernameLoginResponse.code shouldBe SC_UNAUTHORIZED
      val expectedUsernameError = ErrorResponse.usernameNotFoundError(wrongUsername)
      val usernameError = Json.parse(wrongUsernameLoginResponse.body).as[ErrorResponse]
      usernameError shouldBe expectedUsernameError

      And("If I login with a wrong password")
      val wrongPasswordLoginResponse = authenticate(individualCreated.username, wrongPassword)

      Then("The response says that the password does not match")
      wrongPasswordLoginResponse.code shouldBe SC_UNAUTHORIZED
      val expectedPasswordError = ErrorResponse.wrongPasswordError(individualCreated.username)
      val passwordError = Json.parse(wrongPasswordLoginResponse.body).as[ErrorResponse]
      passwordError shouldBe expectedPasswordError
    }

    scenario("Create an organisation") {

      When("I request the creation of an organisation")
      val createdResponse = Http(s"$serviceUrl/organisation").postForm.asString

      Then("The response contains the details of the organisation created")
      createdResponse.code shouldBe SC_CREATED
      val organisationCreated = Json.parse(createdResponse.body).as[CreateTestOrganisationResponse]

      And("The organisation is stored in Mongo with hashed password")
      val organisationFromMongo = result(mongoRepository.fetchByUsername(organisationCreated.username), timeout).get.asInstanceOf[TestOrganisation]
      val expectedOrganisationCreated = CreateTestOrganisationResponse.from(organisationFromMongo.copy(password = organisationCreated.password))
      organisationCreated shouldBe expectedOrganisationCreated
      validatePassword(organisationCreated.password, organisationFromMongo.password) shouldBe true

      And("If I login with the organisation's credentials")
      val loginOrganisationResponse = authenticate(organisationCreated.username, organisationCreated.password)

      Then("The response contains the details of the organisation created previously")
      loginOrganisationResponse.code shouldBe SC_OK
      val organisation = Json.parse(loginOrganisationResponse.body).as[TestOrganisationResponse]
      val expectedOrganisation = TestOrganisationResponse.from(organisationFromMongo)
      organisation shouldBe expectedOrganisation

      Then("The response does not contain the password")
      loginOrganisationResponse.body.toLowerCase shouldNot include("password")

      Then("The response contains 'organisation' as user type")
      loginOrganisationResponse.body.toLowerCase should include("\"usertype\":\"organisation\"")
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

  private def validatePassword(password: String, hashedPassword: String) =  BCryptUtils.checkpw(password, hashedPassword)

  def mongoRepository = {
    implicit val mongo = MongoConnector("mongodb://localhost:27017/api-platform-test-user-it").db
    new TestUserMongoRepository()
  }
}
