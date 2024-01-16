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

import play.api.http.HeaderNames
import play.api.libs.json._
import scalaj.http.Http
import uk.gov.hmrc.testuser.helpers.BaseSpec
import uk.gov.hmrc.testuser.models._
import java.time.format.DateTimeFormatter

class FetchUserSpec extends BaseSpec {
  import TestCreatedResponseReads._

  Feature("Fetch a test user") {

    Scenario("Fetch an individual by NINO") {

      Given("An individual")
      val individual = createIndividual("national-insurance", "self-assessment")

      When("I fetch the individual by its NINO")
      val response = Http(s"$serviceUrl/individuals/nino/${individual.props("nino")}").asString

      Then("The individual is returned")
      Json.parse(response.body) shouldBe Json.parse(
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
      val response = Http(s"$serviceUrl/individuals/shortnino/$shortNino").asString

      Then("The individual is returned")
      Json.parse(response.body) shouldBe Json.parse(
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
      val response = Http(s"$serviceUrl/individuals/sautr/${individual.props("saUtr")}").asString

      Then("The individual is returned")
      Json.parse(response.body) shouldBe Json.parse(
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
      val response = Http(s"$serviceUrl/individuals/vrn/${individual.props("vrn")}").asString

      Then("The individual is returned along with VAT Registration Date")
      Json.parse(response.body) shouldBe Json.parse(
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
      val response      = Http(s"$serviceUrl/organisations/empref/${encodedEmpRef}").asString

      Then("The organisation is returned")
      Json.parse(response.body) shouldBe Json.parse(
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
      val response = Http(s"$serviceUrl/organisations/vrn/${organisation.props("vrn")}").asString

      Then("The organisation is returned along with VAT Registration Date")
      Json.parse(response.body) shouldBe Json.parse(
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
  }

  private def createIndividual(services: String*): TestIndividualCreatedResponse = {
    val createdResponse = Http(s"$serviceUrl/individuals")
      .postData(s"""{ "serviceNames": [${services.map(s => s""" "$s" """).mkString(",")}] }""")
      .header(HeaderNames.CONTENT_TYPE, "application/json").asString
    val json            = Json.parse(createdResponse.body)
    json.as[TestIndividualCreatedResponse]
  }

  private def createOrganisation(services: String*): TestOrganisationCreatedResponse = {
    val createdResponse = Http(s"$serviceUrl/organisations")
      .postData(s"""{ "serviceNames": [${services.map(s => s""" "$s" """).mkString(",")}] }""")
      .header(HeaderNames.CONTENT_TYPE, "application/json").asString
    Json.parse(createdResponse.body).as[TestOrganisationCreatedResponse]
  }
}
