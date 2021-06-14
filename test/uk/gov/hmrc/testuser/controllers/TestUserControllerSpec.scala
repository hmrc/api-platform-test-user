/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.testuser.controllers

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import uk.gov.hmrc.testuser.common.LogSuppressing
import org.joda.time.LocalDate
import play.api.Logger
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{Json, JsValue}
import play.api.libs.json.Json.toJson
import play.api.test._
import play.api.test.Helpers._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models.ServiceKeys.ServiceKey
import uk.gov.hmrc.testuser.models.UserType.{INDIVIDUAL, ORGANISATION}
import uk.gov.hmrc.testuser.services.TestUserService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.{failed, successful}

import uk.gov.hmrc.testuser.common.utils.AsyncHmrcSpec

class TestUserControllerSpec extends AsyncHmrcSpec with LogSuppressing {

  val user = "user"
  val groupIdentifier = "groupIdentifier"
  val password = "password"
  val userFullName = "John Doe"
  val emailAddress = "john.doe@example.com"
  val saUtr = "1555369052"
  val nino = "CC333333C"
  val shortNino = "CC333333"
  val mtdItId = "XGIT00000000054"
  val ctUtr = "1555369053"
  val crn = "12345678"
  val vrn = "999902541"
  val vatRegistrationDate = LocalDate.parse("2011-07-07")
  private val taxOfficeNum = "555"
  private val taxOfficeRef = "EIA000"
  val empRef = s"$taxOfficeNum/$taxOfficeRef"
  val arn = "NARN0396245"
  val lisaManagerReferenceNumber = "Z123456"
  val secureElectronicTransferReferenceNumber = "123456789012"
  val pensionSchemeAdministratorIdentifier = "A1234567"
  val rawEoriNumber = "GB123456789012"
  val eoriNumber = EoriNumber(rawEoriNumber)

  val individualDetails = IndividualDetails("John", "Doe", LocalDate.parse("1980-01-10"), Address("221b Baker St", "Marylebone", "NW1 6XE"))
  val organisationDetails = OrganisationDetails("Company ABCDEF", Address("225 Baker St", "Marylebone", "NW1 6XE"))

  val testIndividual = TestIndividual(
    userId = user,
    password = password,
    userFullName = userFullName,
    emailAddress = emailAddress,
    individualDetails = individualDetails,
    saUtr = Some(saUtr),
    nino = Some(nino),
    mtdItId = Some(mtdItId),
    vrn = Some(vrn),
    vatRegistrationDate = Some(vatRegistrationDate),
    eoriNumber = Some(rawEoriNumber),
    groupIdentifier = Some(groupIdentifier))

  val testOrganisation = TestOrganisation(
    userId = user,
    password = password,
    userFullName = userFullName,
    emailAddress = emailAddress,
    organisationDetails = organisationDetails,
    saUtr = Some(saUtr),
    nino = Some(nino),
    mtdItId = Some(mtdItId),
    empRef = Some(empRef),
    ctUtr = Some(ctUtr),
    vrn = Some(vrn),
    vatRegistrationDate = Some(vatRegistrationDate),
    lisaManRefNum = Some(lisaManagerReferenceNumber),
    secureElectronicTransferReferenceNumber = Some(secureElectronicTransferReferenceNumber),
    pensionSchemeAdministratorIdentifier = Some(pensionSchemeAdministratorIdentifier),
    eoriNumber = Some(rawEoriNumber),
    groupIdentifier = Some(groupIdentifier),
    crn = Some(crn))

  val testAgent = TestAgent(
    user,
    password,
    userFullName,
    emailAddress,
    Some(arn),
    groupIdentifier = Some(groupIdentifier))

  val createIndividualServices = Seq(ServiceKeys.NATIONAL_INSURANCE)
  val createOrganisationServices = Seq(ServiceKeys.NATIONAL_INSURANCE)
  val createAgentServices = Seq(ServiceKeys.AGENT_SERVICES)

  trait Setup {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    implicit val materializer: Materializer = ActorMaterializer()
    implicit val hc = HeaderCarrier()

    val request = FakeRequest()

    def createIndividualRequest = {
      val jsonPayload: JsValue = Json.parse("""{"serviceNames":["national-insurance"]}""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    def createIndividualWithProvidedEoriRequest = {
      val jsonPayload: JsValue = Json.parse(s"""{"serviceNames":["national-insurance"], "eoriNumber": "$rawEoriNumber"}""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    def createOrganisationRequest = {
      val jsonPayload: JsValue = Json.parse("""{"serviceNames":["national-insurance"]}""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    def createOrganisationWithProvidedEoriRequest = {
      val jsonPayload: JsValue = Json.parse(s"""{"serviceNames":["national-insurance"], "eoriNumber": "$rawEoriNumber"}""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    def createAgentRequest = {
      val jsonPayload: JsValue = Json.parse("""{"serviceNames":["agent-services"]}""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    val underTest = new TestUserController(mock[TestUserService], Helpers.stubControllerComponents())
  }

  "createIndividual" should {
    "return 201 (Created) with the created individual" in new Setup {
      when(
        underTest.testUserService.createTestIndividual(eqTo(createIndividualServices), eqTo(None))(any[HeaderCarrier])
      ).thenReturn(successful(testIndividual))

      val result = underTest.createIndividual()(createIndividualRequest)

      status(result) shouldBe CREATED

      contentAsJson(result) shouldBe toJson(
        TestIndividualCreatedResponse(
          user,
          password,
          userFullName,
          emailAddress,
          individualDetails,
          Some(saUtr),
          Some(nino),
          Some(mtdItId),
          Some(vrn),
          Some(vatRegistrationDate),
          Some(rawEoriNumber),
          Some(groupIdentifier)
        )
      )
    }

    "return 201 (Created) with the created individual with provided eori" in new Setup {
      when(
        underTest.testUserService.createTestIndividual(eqTo(createIndividualServices), eqTo(Some(eoriNumber)))(any[HeaderCarrier])
      ).thenReturn(successful(testIndividual))

      val result = underTest.createIndividual()(createIndividualWithProvidedEoriRequest)

      status(result) shouldBe CREATED
    }

    "fail with 500 (Internal Server Error) when the creation of the individual failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        when(
          underTest.testUserService.createTestIndividual(any[Seq[ServiceKey]], any[Option[EoriNumber]])(any[HeaderCarrier])
        ).thenReturn(failed(new RuntimeException("expected test error")))

        val result = underTest.createIndividual()(createIndividualRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR

        contentAsJson(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }

  }

  "createOrganisation" should {

    "return 201 (Created) with the created organisation" in new Setup {

      when(underTest.testUserService.createTestOrganisation(eqTo(createOrganisationServices), eqTo(None))(any[HeaderCarrier])).thenReturn(successful(testOrganisation))

      val result = underTest.createOrganisation()(createOrganisationRequest)

      status(result) shouldBe CREATED
      contentAsJson(result) shouldBe toJson(TestOrganisationCreatedResponse(user, password, userFullName, emailAddress,
        organisationDetails, Some(saUtr),
        Some(nino), Some(mtdItId), Some(empRef), Some(ctUtr), Some(vrn), Some(vatRegistrationDate), Some(lisaManagerReferenceNumber),
        Some(secureElectronicTransferReferenceNumber), Some(pensionSchemeAdministratorIdentifier), Some(rawEoriNumber), Some(groupIdentifier)))
    }

    "return 201 (Created) with the created organisation with provided eori" in new Setup {

      when(underTest.testUserService.createTestOrganisation(eqTo(createOrganisationServices), eqTo(Some(eoriNumber)))(any[HeaderCarrier])).thenReturn(successful(testOrganisation))

      val result = underTest.createOrganisation()(createOrganisationWithProvidedEoriRequest)

      status(result) shouldBe CREATED
    }

    "fail with 500 (Internal Server Error) when the creation of the organisation failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        when(underTest.testUserService.createTestOrganisation(any[Seq[ServiceKey]], any[Option[EoriNumber]])(any[HeaderCarrier]))
          .thenReturn(failed(new RuntimeException("expected test error")))

        val result = underTest.createOrganisation()(createOrganisationRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "createAgent" should {

    "return 201 (Created) with the created agent" in new Setup {

      when(underTest.testUserService.createTestAgent(createAgentServices)).thenReturn(successful(testAgent))

      val result = underTest.createAgent()(createAgentRequest)

      status(result) shouldBe CREATED
      contentAsJson(result) shouldBe toJson(TestAgentCreatedResponse(user, password, userFullName, emailAddress, Some(arn), Some(groupIdentifier)))
    }

    "fail with 500 (Internal Server Error) when the creation of the agent failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        when(underTest.testUserService.createTestAgent(*))
          .thenReturn(failed(new RuntimeException("expected test error")))

        val result = underTest.createAgent()(createAgentRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "fetchIndividualByNino" should {
    "return 200 (Ok) with the individual" in new Setup {

      when(underTest.testUserService.fetchIndividualByNino(eqTo(Nino(nino)))).thenReturn(successful(testIndividual))

      val result = underTest.fetchIndividualByNino(Nino(nino))(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(FetchTestIndividualResponse.from(testIndividual))
    }

    "return a 404 (Not Found) when there is no individual matching the NINO" in new Setup {

      when(underTest.testUserService.fetchIndividualByNino(eqTo(Nino(nino)))).thenReturn(failed(UserNotFound(INDIVIDUAL)))

      val result = underTest.fetchIndividualByNino(Nino(nino))(request)

      status(result) shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.obj("code" -> "USER_NOT_FOUND", "message" -> "The individual can not be found")
    }

    "fail with 500 (Internal Server Error) when fetching the user failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        when(underTest.testUserService.fetchIndividualByNino(eqTo(Nino(nino))))
          .thenReturn(failed(new RuntimeException("expected test error")))

        val result = underTest.fetchIndividualByNino(Nino(nino))(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "fetchIndividualByShortNino" should {
    "return 200 (Ok) with the individual" in new Setup {

      when(underTest.testUserService.fetchIndividualByShortNino(eqTo(NinoNoSuffix(shortNino)))).thenReturn(successful(testIndividual))

      val result = underTest.fetchIndividualByShortNino(NinoNoSuffix(shortNino))(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(FetchTestIndividualResponse.from(testIndividual))
    }

    "return a 404 (Not Found) when there is no individual matching the short nino" in new Setup {

      when(underTest.testUserService.fetchIndividualByShortNino(eqTo(NinoNoSuffix(shortNino)))).thenReturn(failed(UserNotFound(INDIVIDUAL)))

      val result = underTest.fetchIndividualByShortNino(NinoNoSuffix(shortNino))(request)

      status(result) shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.obj("code" -> "USER_NOT_FOUND", "message" -> "The individual can not be found")
    }

    "fail with 500 (Internal Server Error) when fetching the user failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        when(underTest.testUserService.fetchIndividualByShortNino(eqTo(NinoNoSuffix(shortNino))))
          .thenReturn(failed(new RuntimeException("expected test error")))

        val result = underTest.fetchIndividualByShortNino(NinoNoSuffix(shortNino))(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "fetchIndividualBySaUtr" should {
    "return 200 (Ok) with the individual" in new Setup {

      when(underTest.testUserService.fetchIndividualBySaUtr(eqTo(SaUtr(saUtr)))).thenReturn(successful(testIndividual))

      val result = underTest.fetchIndividualBySaUtr(SaUtr(saUtr))(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(FetchTestIndividualResponse.from(testIndividual))
    }

    "return a 404 (Not Found) when there is no individual matching the saUtr" in new Setup {

      when(underTest.testUserService.fetchIndividualBySaUtr(eqTo(SaUtr(saUtr)))).thenReturn(failed(UserNotFound(INDIVIDUAL)))

      val result = underTest.fetchIndividualBySaUtr(SaUtr(saUtr))(request)

      status(result) shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.obj("code" -> "USER_NOT_FOUND", "message" -> "The individual can not be found")
    }

    "fail with 500 (Internal Server Error) when fetching the user failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        when(underTest.testUserService.fetchIndividualBySaUtr(eqTo(SaUtr(saUtr))))
          .thenReturn(failed(new RuntimeException("expected test error")))

        val result = underTest.fetchIndividualBySaUtr(SaUtr(saUtr))(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "fetchOrganisationByEmpref" should {
    "return 200 (Ok) with the organisation" in new Setup {

      when(underTest.testUserService.fetchOrganisationByEmpRef(eqTo(EmpRef.fromIdentifiers(empRef)))).thenReturn(successful(testOrganisation))

      val result = underTest.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef))(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(FetchTestOrganisationResponse.from(testOrganisation))
    }

    "return a 404 (Not Found) when there is no organisation matching the empRef" in new Setup {

      when(underTest.testUserService.fetchOrganisationByEmpRef(eqTo(EmpRef.fromIdentifiers(empRef)))).
        thenReturn(failed(UserNotFound(ORGANISATION)))

      val result = underTest.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef))(request)

      status(result) shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.obj("code" -> "USER_NOT_FOUND", "message" -> "The organisation can not be found")
    }

    "fail with 500 (Internal Server Error) when fetching the user failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        when(underTest.testUserService.fetchOrganisationByEmpRef(eqTo(EmpRef.fromIdentifiers(empRef))))
          .thenReturn(failed(new RuntimeException("expected test error")))

        val result = underTest.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef))(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "fetchOrganisationByCtUtr" should {
    "return 200 (Ok) with the organisation" in new Setup {

      when(underTest.testUserService.fetchOrganisationByCtUtr(eqTo(CtUtr(ctUtr)))).thenReturn(successful(testOrganisation))

      val result = underTest.fetchOrganisationByCtUtr(CtUtr(ctUtr))(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(FetchTestOrganisationResponse.from(testOrganisation))
    }

    "return a 404 (Not Found) when there is no organisation matching the empRef" in new Setup {

      when(underTest.testUserService.fetchOrganisationByCtUtr(eqTo(CtUtr(ctUtr)))).
        thenReturn(failed(UserNotFound(ORGANISATION)))

      val result = underTest.fetchOrganisationByCtUtr(CtUtr(ctUtr))(request)

      status(result) shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.obj("code" -> "USER_NOT_FOUND", "message" -> "The organisation can not be found")
    }

    "fail with 500 (Internal Server Error) when fetching the user failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        when(underTest.testUserService.fetchOrganisationByCtUtr(eqTo(CtUtr(ctUtr))))
          .thenReturn(failed(new RuntimeException("expected test error")))

        val result = underTest.fetchOrganisationByCtUtr(CtUtr(ctUtr))(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "fetchOrganisationBySaUtr" should {
    "return 200 (Ok) with the organisation" in new Setup {

      when(underTest.testUserService.fetchOrganisationBySaUtr(eqTo(SaUtr(saUtr)))).thenReturn(successful(testOrganisation))

      val result = underTest.fetchOrganisationBySaUtr(SaUtr(saUtr))(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(FetchTestOrganisationResponse.from(testOrganisation))
    }

    "return a 404 (Not Found) when there is no organisation matching the empRef" in new Setup {

      when(underTest.testUserService.fetchOrganisationBySaUtr(eqTo(SaUtr(saUtr)))).
        thenReturn(failed(UserNotFound(ORGANISATION)))

      val result = underTest.fetchOrganisationBySaUtr(SaUtr(saUtr))(request)

      status(result) shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.obj("code" -> "USER_NOT_FOUND", "message" -> "The organisation can not be found")
    }

    "fail with 500 (Internal Server Error) when fetching the user failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        when(underTest.testUserService.fetchOrganisationBySaUtr(eqTo(SaUtr(saUtr))))
          .thenReturn(failed(new RuntimeException("expected test error")))

        val result = underTest.fetchOrganisationBySaUtr(SaUtr(saUtr))(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "getServices" should {
    "return the services" in new Setup {
      val result = underTest.getServices()(request)

      contentAsJson(result) shouldBe Json.toJson(Services)
    }
  }
}
