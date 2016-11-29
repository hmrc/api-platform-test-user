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
import org.mindrot.jbcrypt.{BCrypt => BCryptUtils}
import org.scalatest._
import play.api.Mode
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.TestServer
import uk.gov.hmrc.mongo.MongoConnector
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserMongoRepository

import scala.concurrent.Await._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scalaj.http.{Http, HttpResponse}

class TestUserSpec extends FeatureSpec with Matchers
with GivenWhenThen with BeforeAndAfterEach with BeforeAndAfterAll {

  val port = 9999
  val timeout = Duration(5, TimeUnit.SECONDS)
  val serviceUrl = s"http://localhost:$port"
  var server: TestServer = _

  feature("Create a test user") {

    scenario("Create an individual") {

      When("I request the creation of an individual")
      val createdResponse = Http(s"$serviceUrl/individual").postForm.asString

      Then("The response contains the details of the individual created")
      createdResponse.code shouldBe SC_CREATED
      val individualCreated = Json.parse(createdResponse.body).as[TestIndividualCreatedResponse]

      And("The individual is stored in Mongo with hashed password")
      val individualFromMongo = result(mongoRepository.fetchByUsername(individualCreated.username), timeout).get.asInstanceOf[TestIndividual]
      val expectedIndividualCreated = TestIndividualCreatedResponse.from(individualFromMongo.copy(password = individualCreated.password))
      individualCreated shouldBe expectedIndividualCreated
      validatePassword(individualCreated.password, individualFromMongo.password) shouldBe true
    }

    scenario("Create an organisation") {

      When("I request the creation of an organisation")
      val createdResponse = Http(s"$serviceUrl/organisation").postForm.asString

      Then("The response contains the details of the organisation created")
      createdResponse.code shouldBe SC_CREATED
      val organisationCreated = Json.parse(createdResponse.body).as[TestOrganisationCreatedResponse]

      And("The organisation is stored in Mongo with hashed password")
      val organisationFromMongo = result(mongoRepository.fetchByUsername(organisationCreated.username), timeout).get.asInstanceOf[TestOrganisation]
      val expectedOrganisationCreated = TestOrganisationCreatedResponse.from(organisationFromMongo.copy(password = organisationCreated.password))
      organisationCreated shouldBe expectedOrganisationCreated
      validatePassword(organisationCreated.password, organisationFromMongo.password) shouldBe true
    }
  }

  feature("Authenticate test user") {

    def authenticate(usr: String, pwd: String): HttpResponse[String] = {
      val payload = Json.parse(
        s"""
           |{
           |  "username": "$usr",
           |  "password" :"$pwd"
           |}
           |
        """.stripMargin
      ).toString

      Http(s"$serviceUrl/authenticate")
        .headers("Content-type" -> "application/json")
        .postData(payload).asString
    }

    // considering the case of individual only, as for organisations it is similar
    scenario("Valid credentials") {

      When("An individual is created")
      val individualCreatedResponse = Http(s"$serviceUrl/individual").postForm.asString
      val individualCreated = Json.parse(individualCreatedResponse.body).as[TestIndividualCreatedResponse]
      val individualFromMongo = result(mongoRepository.fetchByUsername(individualCreated.username), timeout).get.asInstanceOf[TestIndividual]

      And("If I login with the individual's credentials")
      val loginIndividualResponse = authenticate(individualCreated.username, individualCreated.password)

      Then("The response contains the details of the individual created previously")
      loginIndividualResponse.code shouldBe SC_OK
      val individual = Json.parse(loginIndividualResponse.body).as[TestIndividualResponse]
      val expectedIndividual = TestIndividualResponse.from(individualFromMongo)
      individual shouldBe expectedIndividual

      And("The response does not contain the password")
      loginIndividualResponse.body.toLowerCase shouldNot include("password")

      And("The response contains 'individual' as user type")
      val expectedIndividualResponse = Json.parse(
        s"""
          |{
          |  "username":"${expectedIndividual.username}",
          |  "saUtr":"${expectedIndividual.saUtr}",
          |  "nino":"${expectedIndividual.nino}",
          |  "userType":"INDIVIDUAL"
          |}
        """.stripMargin
      ).toString()

      loginIndividualResponse.body shouldBe expectedIndividualResponse
    }

    // considering the case of individual only, as for organisations it is similar
    scenario("Invalid credentials") {

      val wrongUsername = "WrongUsername"
      val wrongPassword = "WrongPassword"

      When("An individual is created")
      val individualCreatedResponse = Http(s"$serviceUrl/individual").postForm.asString
      val individualCreated = Json.parse(individualCreatedResponse.body).as[TestIndividualCreatedResponse]

      And("If I login with a wrong username")
      val wrongUsernameLoginResponse = authenticate(wrongUsername, individualCreated.password)

      Then("The response says that the credentials are invalid")
      wrongUsernameLoginResponse.code shouldBe SC_UNAUTHORIZED
      val error1 = Json.parse(wrongUsernameLoginResponse.body).as[ErrorResponse]
      error1 shouldBe ErrorResponse.invalidCredentialsError

      And("If I login with a wrong password")
      val wrongPasswordLoginResponse = authenticate(individualCreated.username, wrongPassword)

      Then("The response says that the credentials are invalid")
      wrongPasswordLoginResponse.code shouldBe SC_UNAUTHORIZED
      val error2 = Json.parse(wrongPasswordLoginResponse.body).as[ErrorResponse]
      error2 shouldBe ErrorResponse.invalidCredentialsError
    }
  }

  override def beforeAll = {
    startServer()
  }

  override def beforeEach = {
    result(mongoRepository.drop, timeout)
    result(mongoRepository.ensureIndexes, timeout)
  }

  override def afterAll = {
    stopServer()
    result(mongoRepository.drop, timeout)
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
