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

import sttp.client3.{UriContext, basicRequest}
import sttp.model.StatusCode

import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.libs.json.Json.{obj, stringify}

import uk.gov.hmrc.testuser.helpers.BaseFeatureSpec
import uk.gov.hmrc.testuser.helpers.stubs.AuthLoginApiStub
import uk.gov.hmrc.testuser.models.ErrorResponse.invalidCredentialsError
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models.{ErrorResponse, _}

class AuthenticationSpec extends BaseFeatureSpec {

  Feature("Authenticate a user") {

    Scenario("Valid credentials for an individual") {

      Given("An individual")
      val individual = createIndividual()

      And("The creation of auth session for the individual is successful")
      val authSession = AuthSession("Bearer AUTH_BEARER", "/auth/oid/12345", "gatewayToken")
      AuthLoginApiStub.willReturnTheSession(authSession)

      When("I authenticate with the individual's credentials")
      val response = authenticate((individual \ "userId").as[String], (individual \ "password").as[String])

      Then("The response contains the auth session and the 'Individual' affinity group")
      response.code shouldBe StatusCode.Created
      Json.parse(response.body.value).as[AuthenticationResponse] shouldBe AuthenticationResponse(authSession.gatewayToken, "Individual")
      response.headers(HeaderNames.LOCATION).mkString shouldBe authSession.authorityUri
      response.headers(HeaderNames.AUTHORIZATION).mkString shouldBe authSession.authBearerToken
    }

    Scenario("Valid credentials for an organisation") {

      Given("An organisation")
      val organisation = createOrganisation()

      And("The creation of auth session for the organisation is successful")
      val authSession = AuthSession("Bearer AUTH_BEARER", "/auth/oid/12345", "gatewayToken")
      AuthLoginApiStub.willReturnTheSession(authSession)

      When("I authenticate with the organisation's credentials")
      val response = authenticate((organisation \ "userId").as[String], (organisation \ "password").as[String])

      Then("The response contains the auth session and the 'Organisation' affinity group")
      response.code shouldBe StatusCode.Created
      Json.parse(response.body.value).as[AuthenticationResponse] shouldBe AuthenticationResponse(authSession.gatewayToken, "Organisation")
      response.headers(HeaderNames.LOCATION).mkString shouldBe authSession.authorityUri
      response.headers(HeaderNames.AUTHORIZATION).mkString shouldBe authSession.authBearerToken
    }

    Scenario("Valid credentials for an agent") {

      Given("An agent")
      val agent = createAgent()

      And("The creation of auth session for the agent is successful")
      val authSession = AuthSession("Bearer AUTH_BEARER", "/auth/oid/12345", "gatewayToken")
      AuthLoginApiStub.willReturnTheSession(authSession)

      When("I authenticate with the agents's credentials")
      val response = authenticate((agent \ "userId").as[String], (agent \ "password").as[String])

      Then("The response contains the auth session and the 'Agent' affinity group")
      response.code shouldBe StatusCode.Created
      Json.parse(response.body.value).as[AuthenticationResponse] shouldBe AuthenticationResponse(authSession.gatewayToken, "Agent")
      response.headers(HeaderNames.LOCATION).mkString shouldBe authSession.authorityUri
      response.headers(HeaderNames.AUTHORIZATION).mkString shouldBe authSession.authBearerToken
    }

    Scenario("UserId not found") {

      When("I authenticate with a userId that does not exist")
      val response = authenticate("unknown_user", "password")

      Then("The response says that the credentials are invalid")
      response.code shouldBe StatusCode.Unauthorized
      Json.parse(response.body.left.value).as[ErrorResponse] shouldBe invalidCredentialsError
    }

    Scenario("Invalid password") {

      Given("An individual")
      val individualCreated = createIndividual()

      When("I authenticate with a wrong password")
      val response = authenticate((individualCreated \ "userId").as[String], "wrongPassword")

      Then("The response says that the credentials are invalid")
      response.code shouldBe StatusCode.Unauthorized
      Json.parse(response.body.left.value).as[ErrorResponse] shouldBe invalidCredentialsError
    }
  }

  private def createIndividual() = {
    val individualCreatedResponse = createUser("individuals", Seq("national-insurance"))
    Json.parse(individualCreatedResponse.body.value)
  }

  private def createOrganisation() = {
    val organisationCreatedResponse = createUser("organisations", Seq("national-insurance", "mtd-income-tax"))
    Json.parse(organisationCreatedResponse.body.value)
  }

  private def createAgent() = {
    val agentCreatedResponse = createUser("agents", Seq("agent-services"))
    Json.parse(agentCreatedResponse.body.value)
  }

  private def createUser(endpoint: String, serviceNames: Seq[String]) =
    http(
      basicRequest
        .post(uri"$serviceUrl/$endpoint")
        .body(s"""{ "serviceNames": [${serviceNames.mkString("\"", "\",\"", "\"")}] }""")
        .contentType("application/json")
    )

  private def authenticate(userId: String, password: String) = {
    http(
      basicRequest
        .post(uri"$serviceUrl/session")
        .body(stringify(obj("username" -> userId, "password" -> password)))
        .contentType("application/json")
    )
  }
}
