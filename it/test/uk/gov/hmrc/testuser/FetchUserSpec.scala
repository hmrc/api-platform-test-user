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

import java.net.URLEncoder
import java.time.format.DateTimeFormatter

import sttp.client3.{UriContext, basicRequest}

import play.api.libs.json._

import uk.gov.hmrc.testuser.helpers.BaseFeatureSpec
import uk.gov.hmrc.testuser.models._

class FetchUserSpec extends BaseFeatureSpec {
  import TestCreatedResponseReads._

  Feature("Fetch a test user") {

    Scenario("Fetch an individual by NINO") {

      Given("An individual")
      val individual = createIndividual("national-insurance", "self-assessment")

      When("I fetch the individual by its NINO")
      val response = http(
        basicRequest
          .get(uri"$serviceUrl/individuals/nino/${individual.props("nino")}")
      )

      Then("The individual is returned")
      Json.parse(response.body.value) shouldBe Json.parse(
        s"""{
           |   "userId": "${individual.userId}",
           |   "userFullName": "${individual.userFullName}",
           |   "emailAddress": "${individual.emailAddress}",
           |   "saUtr": "${individual.props("saUtr")}",
           |   "nino": "${individual.props("nino")}",
           |   "groupIdentifier": "${individual.props("groupIdentifier")}",
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

    Scenario("Fetch an individual by SHORTNINO") {

      Given("An individual")
      val individual = createIndividual("national-insurance", "self-assessment")
      val shortNino  = NinoNoSuffix(individual.props("nino").substring(0, 8))

      When("I fetch the individual by its SHORTNINO")
      val response = http(
        basicRequest
          .get(uri"$serviceUrl/individuals/shortnino/$shortNino")
      )

      Then("The individual is returned")
      Json.parse(response.body.value) shouldBe Json.parse(
        s"""{
           |   "userId": "${individual.userId}",
           |   "userFullName": "${individual.userFullName}",
           |   "emailAddress": "${individual.emailAddress}",
           |   "saUtr": "${individual.props("saUtr")}",
           |   "nino": "${individual.props("nino")}",
           |   "groupIdentifier": "${individual.props("groupIdentifier")}",
           |   "individualDetails": ${Json.toJson(individual.individualDetails)}
           |}
      """.stripMargin
      )
    }

    Scenario("Fetch an individual by SAUTR") {

      Given("An individual")
      val individual = createIndividual("national-insurance", "self-assessment")

      When("I fetch the individual by its SAUTR")
      val response = http(
        basicRequest
          .get(uri"$serviceUrl/individuals/sautr/${individual.props("saUtr")}")
      )

      Then("The individual is returned")
      Json.parse(response.body.value) shouldBe Json.parse(
        s"""{
           |   "userId": "${individual.userId}",
           |   "userFullName": "${individual.userFullName}",
           |   "emailAddress": "${individual.emailAddress}",
           |   "saUtr": "${individual.props("saUtr")}",
           |   "nino": "${individual.props("nino")}",
           |   "groupIdentifier": "${individual.props("groupIdentifier")}",
           |   "individualDetails": ${Json.toJson(individual.individualDetails)}
           |}
      """.stripMargin
      )
    }

    Scenario("Fetch an individual by VRN") {

      Given("An individual")
      val individual = createIndividual("mtd-vat")

      When("I fetch the individual by its VRN")
      val response = http(
        basicRequest
          .get(uri"$serviceUrl/individuals/vrn/${individual.props("vrn")}")
      )

      Then("The individual is returned along with VAT Registration Date")
      Json.parse(response.body.value) shouldBe Json.parse(
        s"""{
           |   "userId": "${individual.userId}",
           |   "userFullName": "${individual.userFullName}",
           |   "emailAddress": "${individual.emailAddress}",
           |   "vatRegistrationDate": "${individual.vatRegistrationDate.get.format(DateTimeFormatter.ISO_DATE)}",
           |   "vrn": "${individual.props("vrn")}",
           |   "groupIdentifier": "${individual.props("groupIdentifier")}",
           |   "individualDetails": ${Json.toJson(individual.individualDetails)}
           |}
      """.stripMargin
      )
    }

    Scenario("Fetch an organisation by EMPREF") {

      Given("An organisation")
      val organisation = createOrganisation("paye-for-employers")

      When("I fetch the organisation by its EMPREF")
      val encodedEmpRef = URLEncoder.encode(organisation.props("empRef"), "UTF-8")
      val response      = http(
        basicRequest
          .get(uri"$serviceUrl/organisations/empref/${encodedEmpRef}")
      )

      Then("The organisation is returned")
      Json.parse(response.body.value) shouldBe Json.parse(
        s"""{
           |   "userId": "${organisation.userId}",
           |   "userFullName": "${organisation.userFullName}",
           |   "emailAddress": "${organisation.emailAddress}",
           |   "empRef": "${organisation.props("empRef")}",
           |   "groupIdentifier": "${organisation.props("groupIdentifier")}",
           |   "organisationDetails": {
           |     "name": "${organisation.organisationDetails.name}",
           |     "address": {
           |       "line1": "${organisation.organisationDetails.address.line1}",
           |       "line2": "${organisation.organisationDetails.address.line2}",
           |       "postcode": "${organisation.organisationDetails.address.postcode}"
           |     }
           |   },
           |   "individualDetails": {
           |     "firstName": "${organisation.individualDetails.get.firstName}",
           |     "lastName": "${organisation.individualDetails.get.lastName}",
           |     "dateOfBirth": "${organisation.individualDetails.get.dateOfBirth}",
           |     "address": {
           |       "line1": "${organisation.individualDetails.get.address.line1}",
           |       "line2": "${organisation.individualDetails.get.address.line2}",
           |       "postcode": "${organisation.individualDetails.get.address.postcode}"
           |     }
           |   }
           |}
      """.stripMargin
      )
    }

    Scenario("Fetch an organisation by VRN") {

      Given("An organisation")
      val organisation = createOrganisation("mtd-vat")

      When("I fetch the organisation by its VRN")
      val response = http(
        basicRequest
          .get(uri"$serviceUrl/organisations/vrn/${organisation.props("vrn")}")
      )

      Then("The organisation is returned along with VAT Registration Date")
      Json.parse(response.body.value) shouldBe Json.parse(
        s"""{
           |   "userId": "${organisation.userId}",
           |   "userFullName": "${organisation.userFullName}",
           |   "emailAddress": "${organisation.emailAddress}",
           |   "vrn": "${organisation.props("vrn")}",
           |   "vatRegistrationDate": "${organisation.vatRegistrationDate.get.format(DateTimeFormatter.ISO_DATE)}",
           |   "groupIdentifier": "${organisation.props("groupIdentifier")}",
           |   "organisationDetails": {
           |     "name": "${organisation.organisationDetails.name}",
           |     "address": {
           |       "line1": "${organisation.organisationDetails.address.line1}",
           |       "line2": "${organisation.organisationDetails.address.line2}",
           |       "postcode": "${organisation.organisationDetails.address.postcode}"
           |     }
           |   },
           |   "individualDetails": {
           |     "firstName": "${organisation.individualDetails.get.firstName}",
           |     "lastName": "${organisation.individualDetails.get.lastName}",
           |     "dateOfBirth": "${organisation.individualDetails.get.dateOfBirth}",
           |     "address": {
           |       "line1": "${organisation.individualDetails.get.address.line1}",
           |       "line2": "${organisation.individualDetails.get.address.line2}",
           |       "postcode": "${organisation.individualDetails.get.address.postcode}"
           |     }
           |   }
           |}
      """.stripMargin
      )
    }

    Scenario("Fetch an organisation by Pillar 2 ID") {

      Given("An organisation")
      val organisation = createOrganisation("pillar-2")

      When("I fetch the organisation by its pillar2Id")
      val response = http(
        basicRequest
          .get(uri"$serviceUrl/organisations/pillar2Id/${organisation.props("pillar2Id")}")
      )

      Then("The organisation is returned along with Pillar 2 ID")
      Json.parse(response.body.value) shouldBe Json.parse(
        s"""{
           |   "userId": "${organisation.userId}",
           |   "userFullName": "${organisation.userFullName}",
           |   "emailAddress": "${organisation.emailAddress}",
           |   "organisationDetails": {
           |     "name": "${organisation.organisationDetails.name}",
           |     "address": {
           |       "line1": "${organisation.organisationDetails.address.line1}",
           |       "line2": "${organisation.organisationDetails.address.line2}",
           |       "postcode": "${organisation.organisationDetails.address.postcode}"
           |     }
           |   },
           |   "individualDetails": {
           |     "firstName": "${organisation.individualDetails.get.firstName}",
           |     "lastName": "${organisation.individualDetails.get.lastName}",
           |     "dateOfBirth": "${organisation.individualDetails.get.dateOfBirth}",
           |     "address": {
           |       "line1": "${organisation.individualDetails.get.address.line1}",
           |       "line2": "${organisation.individualDetails.get.address.line2}",
           |       "postcode": "${organisation.individualDetails.get.address.postcode}"
           |     }
           |   },
           |   "pillar2Id": "${organisation.props("pillar2Id")}",
           |   "groupIdentifier": "${organisation.props("groupIdentifier")}"
           |}
      """.stripMargin
      )
    }
  }

  private def createIndividual(services: String*): TestIndividualCreatedResponse = {
    val createdResponse = http(
      basicRequest
        .post(uri"$serviceUrl/individuals")
        .body(s"""{ "serviceNames": [${services.map(s => s""" "$s" """).mkString(",")}] }""")
        .contentType("application/json")
    )
    val json            = Json.parse(createdResponse.body.value)
    json.as[TestIndividualCreatedResponse]
  }

  private def createOrganisation(services: String*): TestOrganisationCreatedResponse = {
    val createdResponse = http(
      basicRequest
        .post(uri"$serviceUrl/organisations")
        .body(s"""{ "serviceNames": [${services.map(s => s""" "$s" """).mkString(",")}] }""")
        .contentType("application/json")
    )
    Json.parse(createdResponse.body.value).as[TestOrganisationCreatedResponse]
  }
}
