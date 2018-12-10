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

package unit.uk.gov.hmrc.testuser.controllers

import common.LogSuppressing
import org.joda.time.LocalDate
import org.mockito.BDDMockito.given
import org.mockito.Matchers.{any, refEq}
import org.scalatest.mockito.MockitoSugar
import play.api.Logger
import play.api.http.HeaderNames
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, UNAUTHORIZED}
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.test._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.testuser.controllers.AuthenticationController
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.ServiceName._
import uk.gov.hmrc.testuser.services.AuthenticationService

import scala.concurrent.Future.{failed, successful}
import uk.gov.hmrc.http.HeaderCarrier

class AuthenticationControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with LogSuppressing {

  val user = "user"
  val password = "password"
  val userFullName = "John Doe"
  val emailAddress = "john.doe@example.com"
  val saUtr = SaUtr("1555369052")
  val nino = Nino("CC333333C")
  val mtdItId = MtdItId("XGIT00000000054")
  val ctUtr = CtUtr("1555369053")
  val vrn = Vrn("999902541")
  val lisaManRefNum = LisaManagerReferenceNumber("Z123456")
  val empRef = EmpRef("555","EIA000")
  val eoriNumber = EoriNumber("GB1234567890")

  val individualDetails = IndividualDetails("John", "Doe", LocalDate.parse("1980-01-10"), Address("221b Baker St", "Marylebone", "NW1 6XE"))
  val testIndividual = TestIndividual(user, password, userFullName, emailAddress, individualDetails,
    Some(saUtr), Some(nino), Some(mtdItId), Some(vrn), Some(eoriNumber),
    Seq(SELF_ASSESSMENT, NATIONAL_INSURANCE, MTD_INCOME_TAX, CUSTOMS_SERVICES, MTD_VAT))

  val organisationDetails = OrganisationDetails("Company ABCDEF",  Address("225 Baker St", "Marylebone", "NW1 6XE"))
  val testOrganisation = TestOrganisation(user, password, userFullName, emailAddress, organisationDetails,
    Some(saUtr), Some(nino), Some(mtdItId), Some(empRef), Some(ctUtr), Some(vrn), Some(lisaManRefNum),
    eoriNumber = Some(eoriNumber),
    services = Seq(SELF_ASSESSMENT, NATIONAL_INSURANCE, MTD_INCOME_TAX, MTD_VAT, PAYE_FOR_EMPLOYERS, CORPORATION_TAX, SUBMIT_VAT_RETURNS, LISA, CUSTOMS_SERVICES))

  val authSession = AuthSession("Bearer AUTH_BEARER", "/auth/oid/12345", "gatewayToken")

  trait Setup {
    implicit lazy val materializer = fakeApplication.materializer
    implicit val hc = HeaderCarrier()

    val createRequest = FakeRequest()

    def authenticationRequest(usr: String, pwd: String) = {
      val jsonPayload: JsValue = Json.parse(s"""{ "username": "$usr", "password" :"$pwd" }""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    val underTest = new AuthenticationController (mock[AuthenticationService])
  }

  "authenticate" should {

    "return 201 (Created), with the auth session and affinity group, when both username and password are correct" in new Setup {

      given(underTest.authenticationService.authenticate(refEq(AuthenticationRequest(user, password)))(any()))
        .willReturn(successful((testIndividual, authSession)))

      val result = await(underTest.authenticate()(authenticationRequest(user, password)))

      status(result) shouldBe CREATED
      jsonBodyOf(result) shouldBe toJson(AuthenticationResponse(authSession.gatewayToken, testIndividual.affinityGroup))
      result.header.headers(HeaderNames.AUTHORIZATION) shouldBe authSession.authBearerToken
      result.header.headers(HeaderNames.LOCATION) shouldBe authSession.authorityUri
    }

    "return 401 (Unauthorized) when the credentials are not valid" in new Setup {

      given(underTest.authenticationService.authenticate(refEq(AuthenticationRequest(user, password)))(any()))
        .willReturn(failed(InvalidCredentials("")))

      val result = await(underTest.authenticate()(authenticationRequest(user, password)))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe toJson(ErrorResponse.invalidCredentialsError)
    }

    "fail with 500 (Internal Server Error) when an error has occurred " in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        given(underTest.authenticationService.authenticate(refEq(AuthenticationRequest(user, password)))(any()))
          .willReturn(failed(new RuntimeException("expected test error")))

        val result = await(underTest.authenticate()(authenticationRequest(user, password)))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe toJson(ErrorResponse.internalServerError)
      }
    }
  }
}
