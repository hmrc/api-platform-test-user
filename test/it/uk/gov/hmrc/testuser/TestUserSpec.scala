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
      val createdResponse = Http(s"$serviceUrl/individuals").postForm.asString

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
      val createdResponse = Http(s"$serviceUrl/organisations").postForm.asString

      Then("The response contains the details of the organisation created")
      createdResponse.code shouldBe SC_CREATED
      val organisationCreated = Json.parse(createdResponse.body).as[TestOrganisationCreatedResponse]

      And("The organisation is stored in Mongo with hashed password")
      val organisationFromMongo = result(mongoRepository.fetchByUserId(organisationCreated.userId), timeout).get.asInstanceOf[TestOrganisation]
      val expectedOrganisationCreated = TestOrganisationCreatedResponse.from(organisationFromMongo.copy(password = organisationCreated.password))
      organisationCreated shouldBe expectedOrganisationCreated
      validatePassword(organisationCreated.password, organisationFromMongo.password) shouldBe true
    }
  }

  private def validatePassword(password: String, hashedPassword: String) =  BCryptUtils.checkpw(password, hashedPassword)
}
