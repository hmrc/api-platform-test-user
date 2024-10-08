/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.testuser

import scala.concurrent.Await._

import org.mindrot.jbcrypt.{BCrypt => BCryptUtils}
import sttp.client3.{UriContext, basicRequest}
import sttp.model.StatusCode

import play.api.libs.json._

import uk.gov.hmrc.testuser.helpers.BaseFeatureSpec
import uk.gov.hmrc.testuser.models.ServiceKey._
import uk.gov.hmrc.testuser.models._

class TestUserISpec extends BaseFeatureSpec {
  import TestCreatedResponseReads._

  Feature("Create a test user") {

    Scenario("Create an individual") {

      When("I request the creation of an individual")
      val createdResponse = createIndividual(Seq("national-insurance"))

      Then("The response contains the details of the individual created")
      createdResponse.code shouldBe StatusCode.Created
      val individualCreated = Json.parse(createdResponse.body.value).as[TestIndividualCreatedResponse]

      And("The individual is stored in Mongo with hashed password")
      val individualFromMongo       = result(mongoRepository.fetchByUserId(individualCreated.userId), timeout).get.asInstanceOf[TestIndividual]
      val expectedIndividualCreated = TestIndividualCreatedResponse.from(individualFromMongo.copy(password = individualCreated.password))
      Json.toJson(individualCreated) shouldBe Json.toJson(expectedIndividualCreated)
      validatePassword(individualCreated.password, individualFromMongo.password) shouldBe true

      And("The individual has the expected services")
      individualFromMongo.services shouldBe Seq(NATIONAL_INSURANCE)
    }

    Scenario("Create an organisation") {

      When("I request the creation of an organisation")
      val createdResponse = createOrganisation(Seq("national-insurance", "mtd-income-tax", "lisa"))

      Then("The response contains the details of the organisation created")
      createdResponse.code shouldBe StatusCode.Created
      val organisationCreated = Json.parse(createdResponse.body.value).as[TestOrganisationCreatedResponse]

      And("The organisation is stored in Mongo with hashed password")
      val organisationFromMongo = result(mongoRepository.fetchByUserId(organisationCreated.userId), timeout).get.asInstanceOf[TestOrganisation]

      val expectedOrganisationCreated = TestOrganisationCreatedResponse.from(organisationFromMongo.copy(password = organisationCreated.password))
      Json.toJson(organisationCreated) shouldBe Json.toJson(expectedOrganisationCreated)
      validatePassword(organisationCreated.password, organisationFromMongo.password) shouldBe true

      And("The organisation has the expected services")
      organisationFromMongo.services shouldBe Seq(NATIONAL_INSURANCE, MTD_INCOME_TAX, LISA)
    }

    Scenario("Create an agent") {

      When("I request the creation of an agent")
      val createdResponse = createAgent(Seq("agent-services"))

      Then("The response contains the details of the agent created")
      createdResponse.code shouldBe StatusCode.Created
      val agentCreated = Json.parse(createdResponse.body.value).as[TestAgentCreatedResponse]

      And("The agent is stored in Mongo with hashed password")
      val agentFromMongo       = result(mongoRepository.fetchByUserId(agentCreated.userId), timeout).get.asInstanceOf[TestAgent]
      val expectedAgentCreated = TestAgentCreatedResponse.from(agentFromMongo.copy(password = agentCreated.password))
      Json.toJson(agentCreated) shouldBe Json.toJson(expectedAgentCreated)
      validatePassword(agentCreated.password, agentFromMongo.password) shouldBe true

      And("The agent has the expected services")
      agentFromMongo.services shouldBe Seq(AGENT_SERVICES)
    }
  }

  private def createIndividual(serviceNames: Seq[String]) = callEndpoint("individuals", serviceNames)

  private def createOrganisation(serviceNames: Seq[String]) = callEndpoint("organisations", serviceNames)

  private def createAgent(serviceNames: Seq[String]) = callEndpoint("agents", serviceNames)

  private def callEndpoint(endpoint: String, serviceNames: Seq[String]) =
    http(
      basicRequest
        .post(uri"$serviceUrl/$endpoint")
        .body(s"""{ "serviceNames": [${serviceNames.mkString("\"", "\",\"", "\"")}] }""")
        .contentType("application/json")
    )

  private def validatePassword(password: String, hashedPassword: String) = BCryptUtils.checkpw(password, hashedPassword)
}
