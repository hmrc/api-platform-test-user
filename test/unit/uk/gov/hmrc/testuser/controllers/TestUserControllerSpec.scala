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

import org.apache.http.HttpStatus.{SC_INTERNAL_SERVER_ERROR, SC_CREATED}
import org.mockito.BDDMockito.given
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
import uk.gov.hmrc.testuser.controllers.TestUserController
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.services.TestUserService

import scala.concurrent.Future.failed

class TestUserControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {
  val testIndividual = TestIndividual("user", "password", SaUtr("1555369052"), Nino("CC333333C"))
  val testOrganisation = TestOrganisation("user", "password", SaUtr("1555369052"), EmpRef("555","EIA000"),
    CtUtr("1555369053"), Vrn("999902541"))

  trait Setup {
    implicit lazy val materializer = fakeApplication.materializer

    val request = FakeRequest()
    val underTest = new TestUserController {
      override val testUserService: TestUserService = mock[TestUserService]
    }
  }

  "createIndividual" should {

    "return 201 (Created) with the created individual" in new Setup {

      given(underTest.testUserService.createTestIndividual()).willReturn(testIndividual)

      val result = await(underTest.createIndividual()(request))

      status(result) shouldBe SC_CREATED
      jsonBodyOf(result) shouldBe Json.toJson(TestIndividualResponse("user", "password", SaUtr("1555369052"), Nino("CC333333C")))
    }

    "fail with 500 (Internal Server Error) when the creation of the individual failed" in new Setup {

      given(underTest.testUserService.createTestIndividual()).willReturn(failed(new RuntimeException()))

      val result = await(underTest.createIndividual()(request))

      status(result) shouldBe SC_INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
    }

  }

  "createOrganisation" should {

    "return 201 (Created) with the created organisation" in new Setup {

      given(underTest.testUserService.createTestOrganisation()).willReturn(testOrganisation)

      val result = await(underTest.createOrganisation()(request))

      status(result) shouldBe SC_CREATED
      jsonBodyOf(result) shouldBe Json.toJson(TestOrganisationResponse("user", "password", SaUtr("1555369052"), EmpRef("555","EIA000"),
        CtUtr("1555369053"), Vrn("999902541")))
    }

    "fail with 500 (Internal Server Error) when the creation of the organisation failed" in new Setup {

      given(underTest.testUserService.createTestOrganisation()).willReturn(failed(new RuntimeException()))

      val result = await(underTest.createOrganisation()(request))

      status(result) shouldBe SC_INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
    }

  }
}
