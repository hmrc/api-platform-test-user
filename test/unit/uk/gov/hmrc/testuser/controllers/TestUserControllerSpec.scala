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

package unit.uk.gov.hmrc.testuser.controllers

import org.mockito.BDDMockito.given
import org.mockito.Matchers.{refEq, any}
import org.scalatest.mock.MockitoSugar
import play.api.http.HeaderNames
import play.api.http.Status.{CREATED, UNAUTHORIZED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.test._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.testuser.controllers.TestUserController
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.services.TestUserService

import scala.concurrent.Future.{failed, successful}

class TestUserControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  val user = "user"
  val password = "password"

  val saUtr = SaUtr("1555369052")
  val nino = Nino("CC333333C")
  val ctUtr = CtUtr("1555369053")
  val vrn = Vrn("999902541")
  val empRef = EmpRef("555","EIA000")

  val testIndividual = TestIndividual(user, password, saUtr, nino)
  val testOrganisation = TestOrganisation(user, password, saUtr, empRef, ctUtr, vrn)

  val authSession = AuthSession("Bearer AUTH_BEARER", "/auth/oid/12345", "gatewayToken")

  trait Setup {
    implicit lazy val materializer = fakeApplication.materializer
    implicit val hc = HeaderCarrier()

    val createRequest = FakeRequest()

    def authenticationRequest(usr: String, pwd: String) = {
      val jsonPayload: JsValue = Json.parse(s"""{ "username": "$usr", "password" :"$pwd" }""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    val underTest = new TestUserController {
      override val testUserService: TestUserService = mock[TestUserService]
    }
  }

  "createIndividual" should {

    "return 201 (Created) with the created individual" in new Setup {

      given(underTest.testUserService.createTestIndividual()).willReturn(testIndividual)

      val result = await(underTest.createIndividual()(createRequest))

      status(result) shouldBe CREATED
      jsonBodyOf(result) shouldBe toJson(TestIndividualCreatedResponse(user, password, saUtr, nino))
    }

    "fail with 500 (Internal Server Error) when the creation of the individual failed" in new Setup {

      given(underTest.testUserService.createTestIndividual()).willReturn(failed(new RuntimeException()))

      val result = await(underTest.createIndividual()(createRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
    }

  }

  "createOrganisation" should {

    "return 201 (Created) with the created organisation" in new Setup {

      given(underTest.testUserService.createTestOrganisation()).willReturn(testOrganisation)

      val result = await(underTest.createOrganisation()(createRequest))

      status(result) shouldBe CREATED
      jsonBodyOf(result) shouldBe toJson(TestOrganisationCreatedResponse(user, password, saUtr, empRef, ctUtr, vrn))
    }

    "fail with 500 (Internal Server Error) when the creation of the organisation failed" in new Setup {

      given(underTest.testUserService.createTestOrganisation()).willReturn(failed(new RuntimeException()))

      val result = await(underTest.createOrganisation()(createRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
    }
  }

  "authenticate" should {

    "return 201 (Created), with the auth session and affinity group, when both username and password are correct" in new Setup {

      given(underTest.testUserService.authenticate(refEq(AuthenticationRequest(user, password)))(any())).willReturn(successful((testIndividual, authSession)))

      val result = await(underTest.authenticate()(authenticationRequest(user, password)))

      status(result) shouldBe CREATED
      jsonBodyOf(result) shouldBe toJson(AuthenticationResponse(authSession.gatewayToken, testIndividual.affinityGroup))
      result.header.headers(HeaderNames.AUTHORIZATION) shouldBe authSession.authBearerToken
      result.header.headers(HeaderNames.LOCATION) shouldBe authSession.authorityUri
    }

    "return 401 (Unauthorized) when the credentials are not valid" in new Setup {

      given(underTest.testUserService.authenticate(refEq(AuthenticationRequest(user, password)))(any())).willReturn(failed(InvalidCredentials("")))

      val result = await(underTest.authenticate()(authenticationRequest(user, password)))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe toJson(ErrorResponse.invalidCredentialsError)
    }

    "fail with 500 (Internal Server Error) when an error occured " in new Setup {

      given(underTest.testUserService.authenticate(refEq(AuthenticationRequest(user, password)))(any())).willReturn(failed(new RuntimeException("test error")))

      val result = await(underTest.authenticate()(authenticationRequest(user, password)))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe toJson(ErrorResponse.internalServerError)
    }
  }
}
