/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.testuser.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlPathEqualTo}
import java.time.LocalDate
import org.scalatest.BeforeAndAfterEach
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.testuser.helpers.stubs.AuthLoginApiStub
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.ServiceKeys._
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.testuser.common.utils.AsyncHmrcSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class AuthLoginApiConnectorSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  val individualDetails = IndividualDetails("John", "Doe", LocalDate.parse("1980-01-10"), Address("221b Baker St", "Marylebone", "NW1 6XE"))
  val organisationDetails = OrganisationDetails("Company ABCDEF", Address("225 Baker St", "Marylebone", "NW1 6XE"))
  val userFullName = "John Doe"
  val emailAddress = "john.doe@example.com"

  val testIndividual = TestIndividual(
    "individualUser",
    "password",
    userFullName,
    emailAddress,
    individualDetails,
    Some("1555369052"),
    vatRegistrationDate = Some(LocalDate.parse("1997-01-01")),
    eoriNumber = Some("GB1234567890"),
    nino = Some("CC444444C"),
    vrn = Some("999902541"),
    mtdItId = Some("XGIT00000000054"),
    groupIdentifier = Some("individualGroup"),
    services = Seq(SELF_ASSESSMENT, NATIONAL_INSURANCE, MTD_INCOME_TAX, CUSTOMS_SERVICES, GOODS_VEHICLE_MOVEMENTS, MTD_VAT,
      CTC_LEGACY, CTC))

  val taxOfficeNumber = "555"

  val taxOfficeReference =  "EIA000"

  val testOrganisation = TestOrganisation(
    userId = "organisationUser",
    password = "password",
    userFullName = userFullName,
    emailAddress = emailAddress,
    organisationDetails = organisationDetails,
    individualDetails = Some(individualDetails),
    saUtr = Some("1555369052"),
    nino = Some("CC333333C"),
    mtdItId = Some("XGIT00000000054"),
    empRef = Some(s"$taxOfficeNumber/$taxOfficeReference"),
    ctUtr = Some("1555369053"),
    vrn = Some("999902541"),
    vatRegistrationDate = Some(LocalDate.parse("1997-01-01")),
    lisaManRefNum = Some("Z123456"),
    secureElectronicTransferReferenceNumber = Some("123456789012"),
    pensionSchemeAdministratorIdentifier = Some("A1234567"),
    eoriNumber = Some("GB1234567890"),
    groupIdentifier = Some("organsiationGroup"),
    crn = Some("12345678"),
    services = Seq(SELF_ASSESSMENT, NATIONAL_INSURANCE, CORPORATION_TAX, SUBMIT_VAT_RETURNS, PAYE_FOR_EMPLOYERS, MTD_INCOME_TAX,
      MTD_VAT, LISA, SECURE_ELECTRONIC_TRANSFER, RELIEF_AT_SOURCE, CUSTOMS_SERVICES, GOODS_VEHICLE_MOVEMENTS,
      SAFETY_AND_SECURITY, CTC_LEGACY, CTC))

  val testAgent = TestAgent(
    userId = "agentUser",
    password = "password",
    userFullName = userFullName,
    emailAddress = emailAddress,
    arn = Some("NARN0396245"),
    agentCode = Some("1234509876"),
    groupIdentifier = Some("agentGroup"),
    services = Seq(AGENT_SERVICES))

  val authSession = AuthSession("Bearer 12345", "/auth/oid/12345", "ggToken")

  trait Setup {
    implicit val hc = HeaderCarrier()

    val underTest = new AuthLoginApiConnector(
      app.injector.instanceOf[HttpClient],
      app.injector.instanceOf[Configuration],
      app.injector.instanceOf[Environment],
      app.injector.instanceOf[ServicesConfig]
    ) {
      override lazy val serviceUrl: String = AuthLoginApiStub.url
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    AuthLoginApiStub.server.start()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    AuthLoginApiStub.server.resetMappings()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    AuthLoginApiStub.server.stop()
  }

  "createSession" should {
    "create a session for an Individual" in new Setup {
      AuthLoginApiStub.willReturnTheSession(authSession)

      val result = await(underTest.createSession(testIndividual))

      result shouldBe authSession
      AuthLoginApiStub.mock.verifyThat(postRequestedFor(urlPathEqualTo("/government-gateway/session/login"))
        .withRequestBody(equalToJson(
          s"""
             |{
             |   "credId": "${testIndividual.userId}",
             |   "affinityGroup": "Individual",
             |   "nino": "${testIndividual.nino.get}",
             |   "confidenceLevel": 200,
             |   "credentialStrength": "strong",
             |   "groupIdentifier": "${testIndividual.groupIdentifier.get}",
             |   "enrolments": [
             |     {
             |       "key": "IR-SA",
             |       "state": "Activated",
             |       "identifiers": [
             |       {
             |         "key":"UTR",
             |         "value":"${testIndividual.saUtr.get}"
             |       }]
             |     },
             |     {
             |       "key": "HMRC-MTD-IT",
             |       "state": "Activated",
             |       "identifiers": [
             |       {
             |         "key":"MTDITID",
             |         "value":"${testIndividual.mtdItId.get}"
             |       }]
             |     },
             |     {
             |       "key": "HMRC-CUS-ORG",
             |       "state": "Activated",
             |       "identifiers": [
             |       {
             |         "key":"EORINumber",
             |         "value":"${testIndividual.eoriNumber.get}"
             |       }]
             |     },
             |     {
             |       "key": "HMRC-GVMS-ORG",
             |       "state": "Activated",
             |       "identifiers": [
             |       {
             |         "key":"EORINumber",
             |         "value":"${testIndividual.eoriNumber.get}"
             |       }]
             |     },
             |     {
             |       "key": "HMRC-MTD-VAT",
             |       "state": "Activated",
             |       "identifiers": [
             |       {
             |         "key":"VRN",
             |         "value":"${testIndividual.vrn.get}"
             |       }]
             |     },
             |     {
             |       "key": "HMCE-NCTS-ORG",
             |       "state": "Activated",
             |       "identifiers": [
             |       {
             |         "key":"VATRegNoTURN",
             |         "value":"${testIndividual.eoriNumber.get}"
             |       }]
             |     },
             |     {
             |       "key": "HMRC-CTC-ORG",
             |       "state": "Activated",
             |       "identifiers": [
             |       {
             |         "key":"EORINumber",
             |         "value":"${testIndividual.eoriNumber.get}"
             |       }]
             |     }
             |   ],
             |   "usersName": "John Doe",
             |   "email": "john.doe@example.com",
             |   "itmpData" : {
             |     "givenName" : "John",
             |     "middleName" : "",
             |     "familyName" : "Doe",
             |     "birthdate" : "1980-01-10",
             |     "address" : {
             |       "line1" : "221b Baker St",
             |       "line2" : "Marylebone",
             |       "postCode" : "NW1 6XE",
             |       "countryName" : "United Kingdom",
             |       "countryCode" : "GB"
             |     }
             |  }
             |}
        """.stripMargin.replaceAll("\n", ""))))
    }

    "create a session for an Organisation" in new Setup {
      AuthLoginApiStub.willReturnTheSession(authSession)

      val result = await(underTest.createSession(testOrganisation))

      result shouldBe authSession
      AuthLoginApiStub.mock.verifyThat(postRequestedFor(urlPathEqualTo("/government-gateway/session/login")).withRequestBody(equalToJson(
        s"""
           |{
           |   "credId": "${testOrganisation.userId}",
           |   "affinityGroup": "Organisation",
           |   "nino": "${testOrganisation.nino.get}",
           |   "confidenceLevel": 200,
           |   "credentialStrength": "strong",
           |   "groupIdentifier": "${testOrganisation.groupIdentifier.get}",
           |   "itmpData" : {
           |     "givenName" : "John",
           |     "middleName" : "",
           |     "familyName" : "Doe",
           |     "birthdate" : "1980-01-10",
           |     "address" : {
           |       "line1" : "221b Baker St",
           |       "line2" : "Marylebone",
           |       "postCode" : "NW1 6XE",
           |       "countryName" : "United Kingdom",
           |       "countryCode" : "GB"
           |     }
           |   },
           |   "enrolments": [
           |     {
           |       "key": "IR-SA",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"UTR",
           |         "value":"${testOrganisation.saUtr.get}"
           |       }]
           |     },
           |     {
           |       "key": "IR-CT",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"UTR",
           |         "value":"${testOrganisation.ctUtr.get}"
           |       }]
           |     },
           |     {
           |       "key": "HMCE-VATDEC-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"VATRegNo",
           |         "value":"${testOrganisation.vrn.get}"
           |       }]
           |     },
           |     {
           |       "key": "IR-PAYE",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"TaxOfficeNumber",
           |         "value":"${taxOfficeNumber}"
           |       },
           |       {
           |         "key":"TaxOfficeReference",
           |         "value":"${taxOfficeReference}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-MTD-IT",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"MTDITID",
           |         "value":"${testOrganisation.mtdItId.get}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-MTD-VAT",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"VRN",
           |         "value":"${testOrganisation.vrn.get}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-LISA-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"ZREF",
           |         "value":"${testOrganisation.lisaManRefNum.get}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-SET-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"SRN",
           |         "value":"${testOrganisation.secureElectronicTransferReferenceNumber.get}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-PSA-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"PSAID",
           |         "value":"${testOrganisation.pensionSchemeAdministratorIdentifier.get}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-CUS-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"EORINumber",
           |         "value":"${testOrganisation.eoriNumber.get}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-GVMS-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"EORINumber",
           |         "value":"${testOrganisation.eoriNumber.get}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-SS-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"EORINumber",
           |         "value":"${testOrganisation.eoriNumber.get}"
           |       }]
           |     },
           |     {
           |       "key": "HMCE-NCTS-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"VATRegNoTURN",
           |         "value":"${testIndividual.eoriNumber.get}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-CTC-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"EORINumber",
           |         "value":"${testOrganisation.eoriNumber.get}"
           |       }]
           |     }
           |   ],
           |   "usersName": "John Doe",
           |   "email": "john.doe@example.com"
           |}
        """.stripMargin.replaceAll("\n", ""))))
    }

    "create a session for an Agent" in new Setup {
      AuthLoginApiStub.willReturnTheSession(authSession)

      val result = await(underTest.createSession(testAgent))

      result shouldBe authSession
      AuthLoginApiStub.mock.verifyThat(postRequestedFor(urlPathEqualTo("/government-gateway/session/login"))
        .withRequestBody(equalToJson(
          s"""
             |{
             |   "credId": "${testAgent.userId}",
             |   "affinityGroup": "Agent",
             |   "confidenceLevel": 200,
             |   "credentialStrength": "strong",
             |   "credentialRole": "user",
             |   "groupIdentifier": "${testAgent.groupIdentifier.get}",
             |   "agentCode" : "1234509876",
             |   "enrolments": [
             |     {
             |       "key": "HMRC-AS-AGENT",
             |       "state": "Activated",
             |       "identifiers": [
             |       {
             |         "key":"AgentReferenceNumber",
             |         "value":"${testAgent.arn.get}"
             |       }]
             |     }
             |   ],
             |   "usersName": "John Doe",
             |   "email": "john.doe@example.com"
             |}
      """.stripMargin.replaceAll("\n", ""))))
    }

    "fail with Upstream5xxResponse when auth-login-api returns an error" in new Setup {
      AuthLoginApiStub.willFailToReturnASession()

      intercept[UpstreamErrorResponse] {
        await(underTest.createSession(testOrganisation))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
