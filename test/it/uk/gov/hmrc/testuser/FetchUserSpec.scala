/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models._

import scalaj.http.Http

class FetchUserSpec extends BaseSpec {

  feature("Fetch a test user") {

    scenario("Fetch an individual by NINO") {

      Given("An individual")
      val individual = createIndividual()

      When("I fetch the individual by its NINO")
      val response = Http(s"$serviceUrl/individuals/nino/${individual.nino.get}").asString

      Then("The individual is returned")
      Json.parse(response.body) shouldBe Json.parse(
      s"""{
        |   "userId": "${individual.userId}",
        |   "userFullName": "${individual.userFullName}",
        |   "emailAddress": "${individual.emailAddress}",
        |   "saUtr": "${individual.saUtr.get}",
        |   "nino": "${individual.nino.get}",
        |   "individualDetails": {
        |     "firstName": "${individual.individualDetails.firstName}",
        |     "lastName": "${individual.individualDetails.lastName}",
        |     "dateOfBirth": "${individual.individualDetails.dateOfBirth}",
        |     "address": {
        |       "line1": "${individual.individualDetails.address.line1}",
        |       "line2": "${individual.individualDetails.address.line2}",
        |       "postcode": "${individual.individualDetails.address.postcode}"
        |     }
        |   }
        |}
      """.stripMargin
      )
    }

    scenario("Fetch an individual by SHORTNINO") {

      Given("An individual")
      val individual = createIndividual()
      val shortNino = NinoNoSuffix(individual.nino.get)

      When("I fetch the individual by its SHORTNINO")
      val response = Http(s"$serviceUrl/individuals/shortnino/$shortNino").asString

      Then("The individual is returned")
      Json.parse(response.body) shouldBe Json.parse(
        s"""{
            |   "userId": "${individual.userId}",
            |   "userFullName": "${individual.userFullName}",
            |   "emailAddress": "${individual.emailAddress}",
            |   "saUtr": "${individual.saUtr.get}",
            |   "nino": "${individual.nino.get}",
            |   "individualDetails": ${toJson(individual.individualDetails)}
            |}
      """.stripMargin
      )
    }

    scenario("Fetch an individual by SAUTR") {

      Given("An individual")
      val individual = createIndividual()

      When("I fetch the individual by its SAUTR")
      val response = Http(s"$serviceUrl/individuals/sautr/${individual.saUtr.get}").asString

      Then("The individual is returned")
      Json.parse(response.body) shouldBe Json.parse(
        s"""{
            |   "userId": "${individual.userId}",
            |   "userFullName": "${individual.userFullName}",
            |   "emailAddress": "${individual.emailAddress}",
            |   "saUtr": "${individual.saUtr.get}",
            |   "nino": "${individual.nino.get}",
            |   "individualDetails": ${toJson(individual.individualDetails)}
            |}
      """.stripMargin
      )
    }

    scenario("Fetch an organisation by EMPREF") {

      Given("An organisation")
      val organisation = createOrganisation()

      When("I fetch the organisation by its EMPREF")
      val response = Http(s"$serviceUrl/organisations/empref/${organisation.empRef.get.encodedValue}").asString

      Then("The organisation is returned")
      Json.parse(response.body) shouldBe Json.parse(
        s"""{
            |   "userId": "${organisation.userId}",
            |   "userFullName": "${organisation.userFullName}",
            |   "emailAddress": "${organisation.emailAddress}",
            |   "empRef": "${organisation.empRef.get}",
            |   "organisationDetails": {
            |     "name": "${organisation.organisationDetails.name}",
            |     "address": {
            |       "line1": "${organisation.organisationDetails.address.line1}",
            |       "line2": "${organisation.organisationDetails.address.line2}",
            |       "postcode": "${organisation.organisationDetails.address.postcode}"
            |     }
            |   }
            |}
      """.stripMargin
      )
    }
  }

  private def createIndividual(): TestIndividualCreatedResponse = {
    val createdResponse = Http(s"$serviceUrl/individuals")
      .postData(s"""{ "serviceNames": ["national-insurance", "self-assessment"] }""")
      .header(HeaderNames.CONTENT_TYPE, "application/json").asString
    Json.parse(createdResponse.body).as[TestIndividualCreatedResponse]
  }

  private def createOrganisation(): TestOrganisationCreatedResponse = {
    val createdResponse = Http(s"$serviceUrl/organisations")
      .postData(s"""{ "serviceNames": ["paye-for-employers"] }""")
      .header(HeaderNames.CONTENT_TYPE, "application/json").asString
    Json.parse(createdResponse.body).as[TestOrganisationCreatedResponse]
  }
}
