/*
 * Copyright 2016 HM Revenue & Customs
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

import org.apache.http.HttpStatus.{SC_CREATED, SC_INTERNAL_SERVER_ERROR, SC_OK, SC_UNAUTHORIZED}
import org.mockito.Mockito.when
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.test._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.testuser.controllers.TestUserController
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.services.TestUserService

import scala.concurrent.Future.{failed, successful}

class TestUserControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  val user = "user"
  val wrongUser = "wrong-usr"
  val password = "password"
  val hashedPassword = "_(pwd)"
  val wrongPassword = "wrong-pwd"

  val saUtr = SaUtr("1555369052")
  val nino = Nino("CC333333C")
  val ctUtr = CtUtr("1555369053")
  val vrn = Vrn("999902541")
  val empRef = EmpRef("555","EIA000")

  val testIndividual = TestIndividual(user, password, saUtr, nino)
  val testHashedIndividual = TestIndividual(user, hashedPassword, saUtr, nino)

  val testOrganisation = TestOrganisation(user, password, saUtr, empRef, ctUtr, vrn)
  val testHashedOrganisation = TestOrganisation(user, hashedPassword, saUtr, empRef, ctUtr, vrn)

  trait Setup {
    implicit lazy val materializer = fakeApplication.materializer

    val createRequest = FakeRequest()

    def authenticationRequest(usr: String, pwd: String) = {
      val jsonPayload: JsValue = Json.parse(s"""{ "username": "$usr", "password" :"$pwd" }""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    val underTest = new TestUserController {
      override val testUserService: TestUserService = mock[TestUserService]

      when(testUserService.authenticate(any[AuthenticationRequest])).thenReturn(successful(None))
    }
  }

  "createIndividual" should {

    "return 201 (Created) with the created individual" in new Setup {

      given(underTest.testUserService.createTestIndividual()).willReturn(testIndividual)

      val result = await(underTest.createIndividual()(createRequest))

      status(result) shouldBe SC_CREATED
      jsonBodyOf(result) shouldBe Json.toJson(TestIndividualCreatedResponse(user, password, saUtr, nino))
    }

    "fail with 500 (Internal Server Error) when the creation of the individual failed" in new Setup {

      given(underTest.testUserService.createTestIndividual()).willReturn(failed(new RuntimeException()))

      val result = await(underTest.createIndividual()(createRequest))

      status(result) shouldBe SC_INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
    }

  }

  "createOrganisation" should {

    "return 201 (Created) with the created organisation" in new Setup {

      given(underTest.testUserService.createTestOrganisation()).willReturn(testOrganisation)

      val result = await(underTest.createOrganisation()(createRequest))

      status(result) shouldBe SC_CREATED
      jsonBodyOf(result) shouldBe Json.toJson(TestOrganisationCreatedResponse(user, password, saUtr, empRef, ctUtr, vrn))
    }

    "fail with 500 (Internal Server Error) when the creation of the organisation failed" in new Setup {

      given(underTest.testUserService.createTestOrganisation()).willReturn(failed(new RuntimeException()))

      val result = await(underTest.createOrganisation()(createRequest))

      status(result) shouldBe SC_INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
    }

  }

  "authenticate" should {

    "return 200, with the right individual, when both username and password are correct" in new Setup {

      when(underTest.testUserService.authenticate(AuthenticationRequest(user, password))).thenReturn(successful(Some(testIndividual)))

      val result = await(underTest.authenticate()(authenticationRequest(user, password)))

      status(result) shouldBe SC_OK
      jsonBodyOf(result) shouldBe Json.toJson(TestIndividualResponse(user, saUtr, nino))
    }

    "return 200, with the right organisation, when both username and password are correct" in new Setup {

      when(underTest.testUserService.authenticate(AuthenticationRequest(user, password))).thenReturn(successful(Some(testOrganisation)))

      val result = await(underTest.authenticate()(authenticationRequest(user, password)))

      status(result) shouldBe SC_OK
      jsonBodyOf(result) shouldBe Json.toJson(TestOrganisationResponse(user, saUtr, empRef, ctUtr, vrn))
    }

    "return 401, if the username does not exist in the users repository" in new Setup {

      val result = await(underTest.authenticate()(authenticationRequest(wrongUser, password)))

      status(result) shouldBe SC_UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.toJson(ErrorResponse.invalidCredentialsError)
    }

    "return 401, if the given username exists, but the password does not match" in new Setup {

      val result = await(underTest.authenticate()(authenticationRequest(user, wrongPassword)))

      status(result) shouldBe SC_UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.toJson(ErrorResponse.invalidCredentialsError)
    }

  }
}
