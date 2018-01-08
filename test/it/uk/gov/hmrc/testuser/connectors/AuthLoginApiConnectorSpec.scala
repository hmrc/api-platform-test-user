/*
 * Copyright 2018 HM Revenue & Customs
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

package it.uk.gov.hmrc.testuser.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlPathEqualTo}
import it.uk.gov.hmrc.testuser.helpers.stubs.AuthLoginApiStub
import org.joda.time.LocalDate
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.testuser.connectors.AuthLoginApiConnector
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.ServiceName._
import uk.gov.hmrc.http.{ HeaderCarrier, Upstream5xxResponse }

class AuthLoginApiConnectorSpec extends UnitSpec with BeforeAndAfterEach with WithFakeApplication {

  val individualDetails = IndividualDetails("John", "Doe", LocalDate.parse("1980-01-10"), Address("221b Baker St", "Marylebone", "NW1 6XE"))
  val organisationDetails = OrganisationDetails("Company ABCDEF",  Address("225 Baker St", "Marylebone", "NW1 6XE"))
  val userFullName = "John Doe"
  val emailAddress = "john.doe@example.com"

  val testIndividual = TestIndividual("individualUser", "password", userFullName, emailAddress, individualDetails, Some(SaUtr("1555369052")), Some(Nino("CC333333C")),
    Some(MtdItId("XGIT00000000054")), Some(EoriNumber("GB1234567890")), Seq(SELF_ASSESSMENT, NATIONAL_INSURANCE, MTD_INCOME_TAX, CUSTOMS_SERVICES))

  val testOrganisation = TestOrganisation("organisationUser", "password", userFullName, emailAddress,  organisationDetails, Some(SaUtr("1555369052")), Some(Nino("CC333333C")),
    Some(MtdItId("XGIT00000000054")), Some(EmpRef("555","EIA000")), Some(CtUtr("1555369053")), Some(Vrn("999902541")),
    Some(LisaManagerReferenceNumber("Z123456")), Some(SecureElectronicTransferReferenceNumber("123456789012")),
    Some(PensionSchemeAdministratorIdentifier("A1234567")), Some(EoriNumber("GB1234567890")),
    Seq(SELF_ASSESSMENT, NATIONAL_INSURANCE, CORPORATION_TAX, SUBMIT_VAT_RETURNS, PAYE_FOR_EMPLOYERS, MTD_INCOME_TAX,
      LISA, SECURE_ELECTRONIC_TRANSFER, RELIEF_AT_SOURCE, CUSTOMS_SERVICES))

  val testAgent = TestAgent("agentUser", "password", userFullName, emailAddress, Some(AgentBusinessUtr("NARN0396245")), Seq(AGENT_SERVICES))

  val authSession = AuthSession("Bearer 12345", "/auth/oid/12345", "ggToken")

  trait Setup {
    implicit val hc = HeaderCarrier()

    val underTest = new AuthLoginApiConnector {
      override lazy val serviceUrl: String = AuthLoginApiStub.url
    }
  }

  override def beforeAll() = {
    super.beforeAll()
    AuthLoginApiStub.server.start()
  }

  override def beforeEach() = {
    super.beforeEach()
    AuthLoginApiStub.server.resetMappings()
  }

  override def afterAll() = {
    super.afterAll()
    AuthLoginApiStub.server.stop()
  }

  "createSession" should {
    "create a session for an Individual" in new Setup {
      AuthLoginApiStub.willReturnTheSession(authSession)

      val result = await(underTest.createSession(testIndividual))

      result shouldBe authSession
      AuthLoginApiStub.mock.verifyThat(postRequestedFor(urlPathEqualTo("/government-gateway/legacy/login"))
        .withRequestBody(equalToJson(
        s"""
          |{
          |   "credId": "${testIndividual.userId}",
          |   "affinityGroup": "Individual",
          |   "nino": "${testIndividual.nino.get}",
          |   "confidenceLevel": 200,
          |   "credentialStrength": "strong",
          |   "enrolments": [
          |     {
          |       "key": "IR-SA",
          |       "state": "Activated",
          |       "identifiers": [
          |       {
          |         "key":"UTR",
          |         "value":"${testIndividual.saUtr.get.value}"
          |       }]
          |     },
          |     {
          |       "key": "HMRC-MTD-IT",
          |       "state": "Activated",
          |       "identifiers": [
          |       {
          |         "key":"MTDITID",
          |         "value":"${testIndividual.mtdItId.get.value}"
          |       }]
          |     },
          |     {
          |       "key": "HMRC-CUS-ORG",
          |       "state": "Activated",
          |       "identifiers": [
          |       {
          |         "key":"EORINumber",
          |         "value":"${testIndividual.eoriNumber.get.value}"
          |       }]
          |     }
          |   ],
          |   "usersName": "John Doe",
          |   "email": "john.doe@example.com"
          |}
        """.stripMargin.replaceAll("\n", ""))))
    }

    "create a session for an Organisation" in new Setup {
      AuthLoginApiStub.willReturnTheSession(authSession)

      val result = await(underTest.createSession(testOrganisation))

      result shouldBe authSession
      AuthLoginApiStub.mock.verifyThat(postRequestedFor(urlPathEqualTo("/government-gateway/legacy/login")).withRequestBody(equalToJson(
        s"""
           |{
           |   "credId": "${testOrganisation.userId}",
           |   "affinityGroup": "Organisation",
           |   "nino": "${testOrganisation.nino.get}",
           |   "confidenceLevel": 200,
           |   "credentialStrength": "strong",
           |   "enrolments": [
           |     {
           |       "key": "IR-SA",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"UTR",
           |         "value":"${testOrganisation.saUtr.get.value}"
           |       }]
           |     },
           |     {
           |       "key": "IR-CT",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"UTR",
           |         "value":"${testOrganisation.ctUtr.get.value}"
           |       }]
           |     },
           |     {
           |       "key": "HMCE-VATDEC-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"VATRegNo",
           |         "value":"${testOrganisation.vrn.get.value}"
           |       }]
           |     },
           |     {
           |       "key": "IR-PAYE",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"TaxOfficeNumber",
           |         "value":"${testOrganisation.empRef.get.taxOfficeNumber}"
           |       },
           |       {
           |         "key":"TaxOfficeReference",
           |         "value":"${testOrganisation.empRef.get.taxOfficeReference}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-MTD-IT",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"MTDITID",
           |         "value":"${testOrganisation.mtdItId.get.value}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-LISA-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"ZREF",
           |         "value":"${testOrganisation.lisaManRefNum.get.value}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-SET-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"SRN",
           |         "value":"${testOrganisation.secureElectronicTransferReferenceNumber.get.value}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-PSA-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"PSAID",
           |         "value":"${testOrganisation.pensionSchemeAdministratorIdentifier.get.value}"
           |       }]
           |     },
           |     {
           |       "key": "HMRC-CUS-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"EORINumber",
           |         "value":"${testOrganisation.eoriNumber.get.value}"
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
      AuthLoginApiStub.mock.verifyThat(postRequestedFor(urlPathEqualTo("/government-gateway/legacy/login"))
        .withRequestBody(equalToJson(
          s"""
             |{
             |   "credId": "${testAgent.userId}",
             |   "affinityGroup": "Agent",
             |   "confidenceLevel": 200,
             |   "credentialStrength": "strong",
             |   "credentialRole": "user",
             |   "enrolments": [
             |     {
             |       "key": "HMRC-AS-AGENT",
             |       "state": "Activated",
             |       "identifiers": [
             |       {
             |         "key":"AgentReferenceNumber",
             |         "value":"${testAgent.arn.get.value}"
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

      intercept[Upstream5xxResponse]{await(underTest.createSession(testOrganisation))}
    }
  }
}
