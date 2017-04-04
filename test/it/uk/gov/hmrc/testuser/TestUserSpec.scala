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

package it.uk.gov.hmrc.testuser


import it.uk.gov.hmrc.testuser.helpers.BaseSpec
import it.uk.gov.hmrc.testuser.helpers.stubs.AuthLoginApiStub
import org.apache.http.HttpStatus._
import org.mindrot.jbcrypt.{BCrypt => BCryptUtils}
import play.api.http.HeaderNames
import play.api.http.Status.{CREATED, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.libs.json.Json.{obj, stringify}
import uk.gov.hmrc.testuser.models.ErrorResponse.invalidCredentialsError
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models._

import scala.concurrent.Await._
import scalaj.http.Http

class TestUserSpec extends BaseSpec {

  feature("Create a test user") {

    scenario("Create an individual") {

      When("I request the creation of an individual")
      val createdResponse = Http(s"$serviceUrl/individual").postForm.asString

      Then("The response contains the details of the individual created")
      createdResponse.code shouldBe SC_CREATED
      val individualCreated = Json.parse(createdResponse.body).as[TestIndividualCreatedResponse]

      And("The individual is stored in Mongo with hashed password")
      val individualFromMongo = result(mongoRepository.fetchByUserId(individualCreated.userId), timeout).get.asInstanceOf[TestIndividual]
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
      val organisationFromMongo = result(mongoRepository.fetchByUserId(organisationCreated.userId), timeout).get.asInstanceOf[TestOrganisation]
      val expectedOrganisationCreated = TestOrganisationCreatedResponse.from(organisationFromMongo.copy(password = organisationCreated.password))
      organisationCreated shouldBe expectedOrganisationCreated
      validatePassword(organisationCreated.password, organisationFromMongo.password) shouldBe true
    }

    scenario("Create an agent") {

      When("I request the creation of an agent")
      val createdResponse = Http(s"$serviceUrl/agent").postForm.asString

      Then("The response contains the details of the agent created")
      createdResponse.code shouldBe SC_CREATED
      val agentCreated = Json.parse(createdResponse.body).as[TestAgentCreatedResponse]

      And("The agent is stored in Mongo with hashed password")
      val agentFromMongo = result(mongoRepository.fetchByUserId(agentCreated.userId), timeout).get.asInstanceOf[TestAgent]
      val expectedAgentCreated = TestAgentCreatedResponse.from(agentFromMongo.copy(password = agentCreated.password))
      agentCreated shouldBe expectedAgentCreated
      validatePassword(agentCreated.password, agentFromMongo.password) shouldBe true
    }
  }

  feature("Authenticate a user") {

    scenario("Valid credentials for an individual") {

      Given("An individual")
      val individual = createIndividual()

      And("The creation of auth session for the individual is successful")
      val authSession = AuthSession("Bearer AUTH_BEARER", "/auth/oid/12345", "gatewayToken")
      AuthLoginApiStub.willReturnTheSession(authSession)

      When("I authenticate with the individual's credentials")
      val response = authenticate(individual.userId, individual.password)

      Then("The response contains the auth session and the 'Individual' affinity group")
      response.code shouldBe CREATED
      Json.parse(response.body).as[AuthenticationResponse] shouldBe AuthenticationResponse(authSession.gatewayToken, "Individual")
      response.headers(HeaderNames.LOCATION) shouldBe authSession.authorityUri
      response.headers(HeaderNames.AUTHORIZATION) shouldBe authSession.authBearerToken
    }

    scenario("Valid credentials for an organisation") {

      Given("An organisation")
      val organisation = createOrganisation()

      And("The creation of auth session for the organisation is successful")
      val authSession = AuthSession("Bearer AUTH_BEARER", "/auth/oid/12345", "gatewayToken")
      AuthLoginApiStub.willReturnTheSession(authSession)

      When("I authenticate with the organisation's credentials")
      val response = authenticate(organisation.userId, organisation.password)

      Then("The response contains the auth session and the 'Organisation' affinity group")
      response.code shouldBe CREATED
      Json.parse(response.body).as[AuthenticationResponse] shouldBe AuthenticationResponse(authSession.gatewayToken, "Organisation")
      response.headers(HeaderNames.LOCATION) shouldBe authSession.authorityUri
      response.headers(HeaderNames.AUTHORIZATION) shouldBe authSession.authBearerToken
    }

    scenario("Valid credentials for an agent") {

      Given("An agent")
      val agent = createAgent()

      And("The creation of auth session for the agent is successful")
      val authSession = AuthSession("Bearer AUTH_BEARER", "/auth/oid/12345", "gatewayToken")
      AuthLoginApiStub.willReturnTheSession(authSession)

      When("I authenticate with the agents's credentials")
      val response = authenticate(agent.userId, agent.password)

      Then("The response contains the auth session and the 'Agent' affinity group")
      response.code shouldBe CREATED
      Json.parse(response.body).as[AuthenticationResponse] shouldBe AuthenticationResponse(authSession.gatewayToken, "Agent")
      response.headers(HeaderNames.LOCATION) shouldBe authSession.authorityUri
      response.headers(HeaderNames.AUTHORIZATION) shouldBe authSession.authBearerToken
    }

    scenario("UserId not found") {

      When("I authenticate with a userId that does not exist")
      val response = authenticate("unknown_user", "password")

      Then("The response says that the credentials are invalid")
      response.code shouldBe SC_UNAUTHORIZED
      Json.parse(response.body).as[ErrorResponse] shouldBe invalidCredentialsError
    }

    scenario("Invalid password") {

      Given("An individual")
      val individualCreated = createIndividual()

      When("I authenticate with a wrong password")
      val response = authenticate(individualCreated.userId, "wrongPassword")

      Then("The response says that the credentials are invalid")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body).as[ErrorResponse] shouldBe invalidCredentialsError
    }
  }

  private def createIndividual() = {
    val individualCreatedResponse = Http(s"$serviceUrl/individual").postForm.asString
    Json.parse(individualCreatedResponse.body).as[TestIndividualCreatedResponse]
  }

  private def createOrganisation() = {
    val organisationCreatedResponse = Http(s"$serviceUrl/organisation").postForm.asString
    Json.parse(organisationCreatedResponse.body).as[TestOrganisationCreatedResponse]
  }

  private def createAgent() = {
    val agentCreatedResponse = Http(s"$serviceUrl/agent").postForm.asString
    Json.parse(agentCreatedResponse.body).as[TestAgentCreatedResponse]
  }

  private def authenticate(userId: String, password: String) = {
    Http(s"$serviceUrl/session")
      .postData(stringify(obj("userId" -> userId, "password" -> password)))
      .header(HeaderNames.CONTENT_TYPE, "application/json").asString
  }

  private def validatePassword(password: String, hashedPassword: String) =  BCryptUtils.checkpw(password, hashedPassword)
}
