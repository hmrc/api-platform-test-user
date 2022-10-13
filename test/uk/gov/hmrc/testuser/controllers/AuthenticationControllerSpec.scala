/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.testuser.common.LogSuppressing
import org.joda.time.LocalDate
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, UNAUTHORIZED}
import play.api.libs.json.{Json, JsValue}
import play.api.libs.json.Json.toJson
import play.api.test._
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models.ServiceKeys._
import uk.gov.hmrc.testuser.services.AuthenticationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.{failed, successful}

import uk.gov.hmrc.testuser.common.utils.AsyncHmrcSpec
import play.api.http.HeaderNames


class AuthenticationControllerSpec extends AsyncHmrcSpec with LogSuppressing {

  val user = "user"
  val groupIdentifier = "groupIdentifier"
  val password = "password"
  val userFullName = "John Doe"
  val emailAddress = "john.doe@example.com"
  val saUtr = "1555369052"
  val nino = "CC333333C"
  val mtdItId = "XGIT00000000054"
  val ctUtr = "1555369053"
  val crn = "12345678"
  val vrn = "999902541"
  val vatRegistrationDate = LocalDate.parse("2016-12-31")
  val lisaManRefNum = "Z123456"
  private val taxOfficeNum = "555"
  private val taxOfficeRef = "EIA000"
  val empRef = s"$taxOfficeNum/$taxOfficeRef"
  val eoriNumber = "GB1234567890"

  val individualDetails = IndividualDetails("John", "Doe", LocalDate.parse("1980-01-10"), Address("221b Baker St", "Marylebone", "NW1 6XE"))
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
    eoriNumber = Some(eoriNumber),
    groupIdentifier = Some(groupIdentifier),
    services = Seq(SELF_ASSESSMENT, NATIONAL_INSURANCE, MTD_INCOME_TAX, CUSTOMS_SERVICES, GOODS_VEHICLE_MOVEMENTS, MTD_VAT, CTC, CTC_LEGACY))

  val organisationDetails = OrganisationDetails(
    name = "Company ABCDEF",
    address = Address("225 Baker St", "Marylebone", "NW1 6XE")
  )

  val testOrganisation = TestOrganisation(
    userId = user,
    password = password,
    userFullName = userFullName,
    emailAddress = emailAddress,
    organisationDetails = organisationDetails,
    individualDetails = Some(individualDetails),
    saUtr = Some(saUtr),
    nino = Some(nino),
    mtdItId = Some(mtdItId),
    empRef = Some(empRef),
    ctUtr = Some(ctUtr),
    vrn = Some(vrn),
    vatRegistrationDate = Some(vatRegistrationDate),
    lisaManRefNum = Some(lisaManRefNum),
    eoriNumber = Some(eoriNumber),
    groupIdentifier = Some(groupIdentifier),
    crn = Some(crn),
    services =
      Seq(SELF_ASSESSMENT, NATIONAL_INSURANCE, MTD_INCOME_TAX, MTD_VAT, PAYE_FOR_EMPLOYERS, CORPORATION_TAX, SUBMIT_VAT_RETURNS, LISA, CUSTOMS_SERVICES, GOODS_VEHICLE_MOVEMENTS, CTC, CTC_LEGACY))

  val authSession = AuthSession("Bearer AUTH_BEARER", "/auth/oid/12345", "gatewayToken")

  trait Setup {
    implicit val hc = HeaderCarrier()

    val createRequest = FakeRequest()

    def authenticationRequest(usr: String, pwd: String) = {
      val jsonPayload: JsValue = Json.parse(s"""{ "username": "$usr", "password" :"$pwd" }""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    val underTest = new AuthenticationController(mock[AuthenticationService], Helpers.stubControllerComponents())
  }

  "authenticate" should {

    "return 201 (Created), with the auth session and affinity group, when both username and password are correct" in new Setup {

      when(underTest.authenticationService.authenticate(refEq(AuthenticationRequest(user, password)))(*))
        .thenReturn(successful((testIndividual, authSession)))

      val result = underTest.authenticate()(authenticationRequest(user, password))

      status(result) shouldBe CREATED
      contentAsJson(result) shouldBe toJson(AuthenticationResponse(authSession.gatewayToken, testIndividual.affinityGroup))
      header(HeaderNames.AUTHORIZATION, result) shouldBe Some(authSession.authBearerToken)
      header(HeaderNames.LOCATION, result) shouldBe Some(authSession.authorityUri)
    }

    "return 401 (Unauthorized) when the credentials are not valid" in new Setup {

      when(underTest.authenticationService.authenticate(refEq(AuthenticationRequest(user, password)))(*))
        .thenReturn(failed(InvalidCredentials("")))

      val result = underTest.authenticate()(authenticationRequest(user, password))

      status(result) shouldBe UNAUTHORIZED
      contentAsJson(result) shouldBe toJson(ErrorResponse.invalidCredentialsError)
    }

    "fail with 500 (Internal Server Error) when an error has occurred " in new Setup {
      when(underTest.authenticationService.authenticate(refEq(AuthenticationRequest(user, password)))(*))
        .thenReturn(failed(new RuntimeException("expected test error")))

      val result = underTest.authenticate()(authenticationRequest(user, password))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe toJson(ErrorResponse.internalServerError)
    }
  }
}
