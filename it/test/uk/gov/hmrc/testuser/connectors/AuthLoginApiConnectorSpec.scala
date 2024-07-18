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

package uk.gov.hmrc.testuser.connectors

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlPathEqualTo}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import uk.gov.hmrc.testuser.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.testuser.helpers.stubs.AuthLoginApiStub
import uk.gov.hmrc.testuser.models.ServiceKey._
import uk.gov.hmrc.testuser.models._

class AuthLoginApiConnectorSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  val individualDetails   = IndividualDetails("John", "Doe", LocalDate.parse("1980-01-10"), Address("221b Baker St", "Marylebone", "NW1 6XE"))
  val organisationDetails = OrganisationDetails("Company ABCDEF", Address("225 Baker St", "Marylebone", "NW1 6XE"))
  val userFullName        = "John Doe"
  val emailAddress        = "john.doe@example.com"

  val individualProps = Map[TestUserPropKey, String](
    TestUserPropKey.eoriNumber      -> "GB1234567890",
    TestUserPropKey.nino            -> "CC444444C",
    TestUserPropKey.vrn             -> "999902541",
    TestUserPropKey.mtdItId         -> "XGIT00000000054",
    TestUserPropKey.groupIdentifier -> "individualGroup",
    TestUserPropKey.saUtr           -> "1555369052"
  )

  val testIndividual = TestIndividual(
    "individualUser",
    "password",
    userFullName,
    emailAddress,
    individualDetails,
    vatRegistrationDate = Some(LocalDate.parse("1997-01-01")),
    services = Seq(SELF_ASSESSMENT, NATIONAL_INSURANCE, MTD_INCOME_TAX, CUSTOMS_SERVICES, GOODS_VEHICLE_MOVEMENTS, MTD_VAT, CTC_LEGACY, CTC),
    props = individualProps
  )

  val taxOfficeNumber = "555"

  val taxOfficeReference                      = "EIA000"
  val saUtr                                   = "1555369052"
  val nino                                    = "CC333333C"
  val mtdItId                                 = "XGIT00000000054"
  val empRef                                  = s"$taxOfficeNumber/$taxOfficeReference"
  val ctUtr                                   = "1555369053"
  val vrn                                     = "999902541"
  val lisaManRefNum                           = "Z123456"
  val secureElectronicTransferReferenceNumber = "123456789012"
  val pensionSchemeAdministratorIdentifier    = "A1234567"
  val eoriNumber                              = "GB1234567890"
  val exciseNumber                            = "GBWK254706100"
  val groupIdentifier                         = "organsiationGroup"
  val crn                                     = "12345678"

  val orgProps = Map[TestUserPropKey, String](
    TestUserPropKey.saUtr                                   -> saUtr,
    TestUserPropKey.nino                                    -> nino,
    TestUserPropKey.mtdItId                                 -> mtdItId,
    TestUserPropKey.empRef                                  -> empRef,
    TestUserPropKey.ctUtr                                   -> ctUtr,
    TestUserPropKey.vrn                                     -> vrn,
    TestUserPropKey.lisaManRefNum                           -> lisaManRefNum,
    TestUserPropKey.secureElectronicTransferReferenceNumber -> secureElectronicTransferReferenceNumber,
    TestUserPropKey.pensionSchemeAdministratorIdentifier    -> pensionSchemeAdministratorIdentifier,
    TestUserPropKey.eoriNumber                              -> eoriNumber,
    TestUserPropKey.exciseNumber                            -> exciseNumber,
    TestUserPropKey.groupIdentifier                         -> groupIdentifier,
    TestUserPropKey.crn                                     -> crn
  )

  val testOrganisation = TestOrganisation(
    userId = "organisationUser",
    password = "password",
    userFullName = userFullName,
    emailAddress = emailAddress,
    organisationDetails = organisationDetails,
    individualDetails = Some(individualDetails),
    vatRegistrationDate = Some(LocalDate.parse("1997-01-01")),
    services = Seq(
      SELF_ASSESSMENT,
      NATIONAL_INSURANCE,
      CORPORATION_TAX,
      SUBMIT_VAT_RETURNS,
      PAYE_FOR_EMPLOYERS,
      MTD_INCOME_TAX,
      MTD_VAT,
      LISA,
      SECURE_ELECTRONIC_TRANSFER,
      RELIEF_AT_SOURCE,
      CUSTOMS_SERVICES,
      GOODS_VEHICLE_MOVEMENTS,
      SAFETY_AND_SECURITY,
      CTC_LEGACY,
      CTC,
      EMCS
    ),
    props = orgProps
  )

  val testAgent = TestAgent(
    userId = "agentUser",
    password = "password",
    userFullName = userFullName,
    emailAddress = emailAddress,
    services = Seq(AGENT_SERVICES),
    props = Map(
      TestUserPropKey.arn             -> "NARN0396245",
      TestUserPropKey.agentCode       -> "1234509876",
      TestUserPropKey.groupIdentifier -> "agentGroup"
    )
  )

  val authSession = AuthSession("Bearer 12345", "/auth/oid/12345", "ggToken")

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val underTest = new AuthLoginApiConnector(
      app.injector.instanceOf[HttpClientV2],
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
      implicit override val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessions")), deviceID = Some("MyDevice"))

      val result = await(underTest.createSession(testIndividual))

      result shouldBe authSession
      AuthLoginApiStub.mock.verifyThat(postRequestedFor(urlPathEqualTo("/government-gateway/session/login"))
        .withRequestBody(equalToJson(
          s"""
             |{
             |   "credId": "${testIndividual.userId}",
             |   "affinityGroup": "Individual",
             |   "nino": "${testIndividual.nino.get}",
             |   "confidenceLevel": 250,
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
             |  },
             |  "mdtpInformation" :{
             |  "deviceId":"MyDevice",
             |  "sessionId":"sessions"
             |  }
             |}
        """.stripMargin.replaceAll("\n", "")
        )))
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
           |   "confidenceLevel": 250,
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
           |  "mdtpInformation" :{
           |  "deviceId":"TestDeviceId",
           |  "sessionId":"TestSessionId"
           |  },
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
           |     },
           |     {
           |       "key": "HMRC-EMCS-ORG",
           |       "state": "Activated",
           |       "identifiers": [
           |       {
           |         "key":"ExciseNumber",
           |         "value":"${testOrganisation.exciseNumber.get}"
           |       }]
           |     }
           |   ],
           |   "usersName": "John Doe",
           |   "email": "john.doe@example.com"
           |}
        """.stripMargin.replaceAll("\n", "")
      )))
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
             |   "confidenceLevel": 250,
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
             |  "mdtpInformation" :{
             |  "deviceId":"TestDeviceId",
             |  "sessionId":"TestSessionId"
             |  },
             |   "usersName": "John Doe",
             |   "email": "john.doe@example.com"
             |}
      """.stripMargin.replaceAll("\n", "")
        )))
    }

    "fail with Upstream5xxResponse when auth-login-api returns an error" in new Setup {
      AuthLoginApiStub.willFailToReturnASession()

      intercept[UpstreamErrorResponse] {
        await(underTest.createSession(testOrganisation))
      }.statusCode shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
