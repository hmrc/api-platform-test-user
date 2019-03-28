/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.uk.gov.hmrc.testuser.services

import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.connectors.AuthLoginApiConnector
import uk.gov.hmrc.testuser.models.LegacySandboxUser._
import uk.gov.hmrc.testuser.models.ServiceKeys._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.services.{AuthenticationService, PasswordService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

class AuthenticationServiceSpec extends UnitSpec with MockitoSugar {

  val userId = "user"
  val password = "password"
  val userFullName = "John Doe"
  val emailAddress = "john.doe@example.com"
  val hashedPassword = "hashedPassword"
  val authSession = AuthSession("Bearer AUTH_TOKEN", "/auth/oid/12345", "gatewayToken")
  val individualDetails = IndividualDetails("John", "Doe", LocalDate.parse("1980-01-10"), Address("221b Baker St", "Marylebone", "NW1 6XE"))
  val storedTestIndividual = TestIndividual(userId, hashedPassword, userFullName, emailAddress, individualDetails,
    saUtr = Some("1555369052"), nino = Some("CC333333C"), mtdItId = Some("XGIT00000000054"),
    services = Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX))

  trait Setup {
    implicit val hc = HeaderCarrier()

    val underTest = new AuthenticationService(mock[PasswordService], mock[AuthLoginApiConnector], mock[TestUserRepository])

    when(underTest.testUserRepository.fetchByUserId(anyString())).thenReturn(successful(None))
    when(underTest.passwordService.validate(anyString(), anyString())).thenReturn(false)
    when(underTest.passwordService.validate(password, hashedPassword)).thenReturn(true)
  }

  "authenticate" should {

    "return the user and auth session when the credentials are valid" in new Setup {

      given(underTest.testUserRepository.fetchByUserId(userId)).willReturn(Some(storedTestIndividual))
      given(underTest.authLoginApiConnector.createSession(storedTestIndividual)).willReturn(authSession)

      val result = await(underTest.authenticate(AuthenticationRequest(userId, password)))

      result shouldBe storedTestIndividual -> authSession
    }

    "return the user and auth session for the sandbox user when I authenticate with user1/password1" in new Setup {

      given(underTest.authLoginApiConnector.createSession(sandboxUser)).willReturn(authSession)

      val result = await(underTest.authenticate(AuthenticationRequest("user1", "password1")))

      result shouldBe sandboxUser -> authSession
    }

    "fail with InvalidCredentials when the user does not exist" in new Setup {

      given(underTest.testUserRepository.fetchByUserId(userId)).willReturn(None)

      intercept[InvalidCredentials] {
        await(underTest.authenticate(AuthenticationRequest(userId, password)))
      }
    }

    "fail with InvalidCredentials when the password is invalid" in new Setup {
      given(underTest.testUserRepository.fetchByUserId(userId)).willReturn(Some(storedTestIndividual))
      given(underTest.authLoginApiConnector.createSession(storedTestIndividual)).willReturn(authSession)

      intercept[InvalidCredentials] {
        await(underTest.authenticate(AuthenticationRequest(userId, "wrong password")))
      }
    }

  }
}
