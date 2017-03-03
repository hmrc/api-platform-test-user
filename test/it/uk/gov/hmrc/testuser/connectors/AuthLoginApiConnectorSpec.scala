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

package it.uk.gov.hmrc.testuser.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, urlPathEqualTo, postRequestedFor}
import it.uk.gov.hmrc.testuser.helpers.stubs.AuthLoginApiStub
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.http.{Upstream5xxResponse, HeaderCarrier}
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import uk.gov.hmrc.testuser.connectors.AuthLoginApiConnector
import uk.gov.hmrc.testuser.models.{AuthSession, TestOrganisation, TestIndividual}

class AuthLoginApiConnectorSpec extends UnitSpec with BeforeAndAfterEach with WithFakeApplication {

  val testIndividual = TestIndividual("individualUser", "password", SaUtr("1555369052"), Nino("CC333333C"))
  val testOrganisation = TestOrganisation("organisationUser", "password", SaUtr("1555369052"), EmpRef("555","EIA000"),
    CtUtr("1555369053"), Vrn("999902541"))

  val authSession = AuthSession("Bearer 12345", "/auth/oid/12345")

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
          |   "credId": "${testIndividual.username}",
          |   "affinityGroup": "Individual",
          |   "nino": "${testIndividual.nino}",
          |   "confidenceLevel": 200,
          |   "credentialStrength": "strong",
          |   "enrolments": [
          |     {
          |       "key": "IR-SA",
          |       "state": "Activated",
          |       "identifiers": [
          |       {
          |         "key":"UTR",
          |         "value":"${testIndividual.saUtr.value}"
          |       }]
          |     }
          |   ]
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
           |   "credId": "${testOrganisation.username}",
           |   "affinityGroup": "Organisation",
           |   "confidenceLevel": 200,
           |   "credentialStrength": "strong",
           |   "enrolments": [
           |     {
           |       "key": "IR-SA",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"UTR",
           |         "value":"${testOrganisation.saUtr.value}"
           |       }]
           |     },
           |     {
           |       "key": "IR-CT",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"UTR",
           |         "value":"${testOrganisation.ctUtr.value}"
           |       }]
           |     },
           |     {
           |       "key": "HMCE-VATDEC-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"VATRegNo",
           |         "value":"${testOrganisation.vrn.value}"
           |       }]
           |     },
           |     {
           |       "key": "IR-PAYE",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"TaxOfficeNumber",
           |         "value":"${testOrganisation.empRef.taxOfficeNumber}"
           |       },
           |       {
           |         "key":"TaxOfficeReference",
           |         "value":"${testOrganisation.empRef.taxOfficeReference}"
           |       }]
           |     }
           |   ]
           |}
        """.stripMargin.replaceAll("\n", ""))))
    }

    "fail with Upstream5xxResponse when auth-login-api returns an error" in new Setup {
      AuthLoginApiStub.willFailToReturnASession()

      intercept[Upstream5xxResponse]{await(underTest.createSession(testOrganisation))}
    }
  }
}
