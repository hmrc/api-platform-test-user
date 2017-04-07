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

import common.LogSuppressing
import org.mockito.BDDMockito.given
import org.mockito.Matchers.{any, refEq}
import org.scalatest.mock.MockitoSugar
import play.api.Logger
import play.api.http.HeaderNames
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, UNAUTHORIZED}
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

class TestUserControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with LogSuppressing {

  val user = "user"
  val password = "password"

  val saUtr = SaUtr("1555369052")
  val nino = Nino("CC333333C")
  val mtdItId = MtdItId("XGIT00000000054")
  val ctUtr = CtUtr("1555369053")
  val vrn = Vrn("999902541")
  val empRef = EmpRef("555","EIA000")
  val arn = AgentBusinessUtr("NARN0396245")

  val testIndividual = TestIndividual(user, password, Some(saUtr), Some(nino), Some(mtdItId))
  val testOrganisation = TestOrganisation(user, password, Some(saUtr), Some(nino), Some(mtdItId), Some(empRef), Some(ctUtr), Some(vrn))
  val testAgent = TestAgent(user, password, Some(arn))
  val createIndividualServices = Seq(ServiceName.NATIONAL_INSURANCE)
  val createOrganisationServices = Seq(ServiceName.NATIONAL_INSURANCE)
  val createAgentServices = Seq(ServiceName.AGENT_SERVICES)

  trait Setup {
    implicit lazy val materializer = fakeApplication.materializer
    implicit val hc = HeaderCarrier()

    val createRequest = FakeRequest()

    def createIndividualRequest = {
      val jsonPayload: JsValue = Json.parse(s"""{"serviceNames":["national-insurance"]}""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    def createOrganisationRequest = {
      val jsonPayload: JsValue = Json.parse(s"""{"serviceNames":["national-insurance"]}""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    def createAgentRequest = {
      val jsonPayload: JsValue = Json.parse(s"""{"serviceNames":["agent-services"]}""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    def authenticationRequest(usr: String, pwd: String) = {
      val jsonPayload: JsValue = Json.parse(s"""{ "userId": "$usr", "password" :"$pwd" }""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    val underTest = new TestUserController {
      override val testUserService: TestUserService = mock[TestUserService]
    }
  }

  "createIndividual" should {

    "return 201 (Created) with the created individual" in new Setup {

      given(underTest.testUserService.createTestIndividual(refEq(createIndividualServices))(any())).willReturn(testIndividual)

      val result = await(underTest.createIndividual()(createIndividualRequest))

      status(result) shouldBe CREATED
      jsonBodyOf(result) shouldBe toJson(TestIndividualCreatedResponse(user, password, Some(saUtr), Some(nino), Some(mtdItId)))
    }

    "fail with 500 (Internal Server Error) when the creation of the individual failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        given(underTest.testUserService.createTestIndividual(any())(any()))
          .willReturn(failed(new RuntimeException("expected test error")))

        val result = await(underTest.createIndividual()(createIndividualRequest))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }

  }

  "createOrganisation" should {

    "return 201 (Created) with the created organisation" in new Setup {

      given(underTest.testUserService.createTestOrganisation(refEq(createOrganisationServices))(any())).willReturn(testOrganisation)

      val result = await(underTest.createOrganisation()(createOrganisationRequest))

      status(result) shouldBe CREATED
      jsonBodyOf(result) shouldBe toJson(TestOrganisationCreatedResponse(user, password, Some(saUtr),
        Some(nino), Some(mtdItId), Some(empRef), Some(ctUtr), Some(vrn)))
    }

    "fail with 500 (Internal Server Error) when the creation of the organisation failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        given(underTest.testUserService.createTestOrganisation(any())(any()))
          .willReturn(failed(new RuntimeException("expected test error")))

        val result = await(underTest.createOrganisation()(createOrganisationRequest))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "createAgent" should {

    "return 201 (Created) with the created agent" in new Setup {

      given(underTest.testUserService.createTestAgent(createAgentServices)).willReturn(testAgent)

      val result = await(underTest.createAgent()(createAgentRequest))

      status(result) shouldBe CREATED
      jsonBodyOf(result) shouldBe toJson(TestAgentCreatedResponse(user, password, Some(arn)))
    }

    "fail with 500 (Internal Server Error) when the creation of the agent failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        given(underTest.testUserService.createTestAgent(any()))
          .willReturn(failed(new RuntimeException("expected test error")))

        val result = await(underTest.createAgent()(createAgentRequest))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }
}
