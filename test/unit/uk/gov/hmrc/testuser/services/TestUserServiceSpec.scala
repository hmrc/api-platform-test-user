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

package unit.uk.gov.hmrc.testuser.services

import org.mockito.Mockito.{verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.Matchers.{any, anyString}
import org.mockito.BDDMockito.given
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.LegacySandboxUser._
import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.services.{Generator, PasswordService, TestUserService}

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class TestUserServiceSpec extends UnitSpec with MockitoSugar {

  val username = "user"
  val password = "password"
  val testIndividual = TestIndividual(username, password, SaUtr("1555369052"), Nino("CC333333C"))
  val testOrganisation = TestOrganisation(username, password, SaUtr("1555369052"), EmpRef("555","EIA000"),
    CtUtr("1555369053"), Vrn("999902541"))

  trait Setup {
    val underTest = new TestUserService {
      override val generator: Generator = mock[Generator]
      override val testUserRepository: TestUserRepository = mock[TestUserRepository]
      override val passwordService: PasswordService = mock[PasswordService]
    }
    when(underTest.testUserRepository.createUser(any[TestUser]())).thenAnswer(sameUserCreated)
    when(underTest.testUserRepository.fetchByUsername(anyString())).thenReturn(successful(None))
    when(underTest.testUserRepository.fetchByUsername(username)).thenReturn(successful(Some(testOrganisation)))
    when(underTest.passwordService.validate(anyString(), anyString())).thenReturn(false)
    when(underTest.passwordService.validate(password, testOrganisation.password)).thenReturn(true)
  }

  "createTestIndividual" should {

    "Generate an individual and save it with hashed password in the database" in new Setup {

      val hashedPassword  = "hashedPassword"
      given(underTest.generator.generateTestIndividual()).willReturn(testIndividual)
      given(underTest.passwordService.hash(testIndividual.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestIndividual())

      result shouldBe testIndividual
      verify(underTest.testUserRepository).createUser(testIndividual.copy(password = hashedPassword))
    }

    "fail when the repository fails" in new Setup {

      given(underTest.generator.generateTestIndividual()).willReturn(testIndividual)
      given(underTest.testUserRepository.createUser(any[TestUser]())).willReturn(failed(new RuntimeException()))

      intercept[RuntimeException]{await(underTest.createTestIndividual())}
    }
  }

  "createTestOrganisation" should {

    "Generate an organisation and save it in the database" in new Setup {

      val hashedPassword  = "hashedPassword"
      given(underTest.generator.generateTestOrganisation()).willReturn(testOrganisation)
      given(underTest.passwordService.hash(testOrganisation.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestOrganisation())

      result shouldBe testOrganisation
      verify(underTest.testUserRepository).createUser(testOrganisation.copy(password = hashedPassword))
    }

    "fail when the repository fails" in new Setup {

      given(underTest.generator.generateTestOrganisation()).willReturn(testOrganisation)
      given(underTest.testUserRepository.createUser(any[TestUser]())).willReturn(failed(new RuntimeException()))

      intercept[RuntimeException]{await(underTest.createTestIndividual())}
    }
  }

  // considering the organisation case only because the individual case is similar
  "authenticate" should {

    "Give the correct organisation if I use the correct credentials" in new Setup {
      val org = await(underTest.authenticate(AuthenticationRequest(username, password)))
      org shouldBe Some(testOrganisation)
    }

    "Give `None` if the username was not found" in new Setup {
      val org = await(underTest.authenticate(AuthenticationRequest("U", password)))
      org shouldBe None
    }

    "Give `None` if the password does not match" in new Setup {
      val org = await(underTest.authenticate(AuthenticationRequest(username, "P")))
      org shouldBe None
    }

    // legacy sandbox user (user1/password1)
    "Give the legacy sandbox user details if I authenticate with the legacy sandbox credentials" in new Setup {
      val individual = await(underTest.authenticate(sandboxAuthenticationRequest))
      individual shouldBe Some(sandboxUser)
    }
  }

  val sameUserCreated = new Answer[Future[TestUser]] {
    override def answer(invocationOnMock: InvocationOnMock): Future[TestUser] = {
      successful(invocationOnMock.getArguments.head.asInstanceOf[TestUser])
    }
  }
}
