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

package unit.uk.gov.hmrc.testuser.services

import org.mockito.Mockito.{verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.Matchers.{any, anyString}
import org.mockito.BDDMockito.given
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.connectors.{AuthLoginApiConnector, DesSimulatorConnector}
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.LegacySandboxUser._
import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.services.{Generator, PasswordService, TestUserService}

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class TestUserServiceSpec extends UnitSpec with MockitoSugar {

  val userId = "user"
  val password = "password"
  val hashedPassword = "hashedPassword"
  val testIndividual = TestIndividual(userId, password, SaUtr("1555369052"), Nino("CC333333C"),
    MtdId("XGIT00000000054"))
  val testOrganisation = TestOrganisation(userId, password, SaUtr("1555369052"), Nino("CC333333C"),
    MtdId("XGIT00000000054"), EmpRef("555","EIA000"), CtUtr("1555369053"), Vrn("999902541"))
  val testAgent = TestAgent(userId, password, AgentBusinessUtr("NARN0396245"))
  val authSession = AuthSession("Bearer AUTH_TOKEN", "/auth/oid/12345", "gatewayToken")
  val storedTestIndividual = TestIndividual(userId, hashedPassword, SaUtr("1555369052"), Nino("CC333333C"), MtdId("XGIT00000000054"))

  trait Setup {
    implicit val hc = HeaderCarrier()

    val underTest = new TestUserService {
      override val generator: Generator = mock[Generator]
      override val testUserRepository: TestUserRepository = mock[TestUserRepository]
      override val passwordService: PasswordService = mock[PasswordService]
      override val desSimulatorConnector: DesSimulatorConnector = mock[DesSimulatorConnector]
    }
    when(underTest.testUserRepository.createUser(any[TestUser]())).thenAnswer(sameUserCreated)
    when(underTest.testUserRepository.fetchByUserId(anyString())).thenReturn(successful(None))
    when(underTest.passwordService.validate(anyString(), anyString())).thenReturn(false)
    when(underTest.passwordService.validate(password, hashedPassword)).thenReturn(true)
  }

  "createTestIndividual" should {

    "Generate an individual and save it with hashed password in the database" in new Setup {

      val hashedPassword  = "hashedPassword"
      given(underTest.generator.generateTestIndividual()).willReturn(testIndividual)
      given(underTest.passwordService.hash(testIndividual.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestIndividual())

      result shouldBe testIndividual

      val testIndividualWithHashedPassword = testIndividual.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testIndividualWithHashedPassword)
      verify(underTest.desSimulatorConnector).createIndividual(testIndividualWithHashedPassword)
    }

    "fail when the repository fails" in new Setup {

      given(underTest.generator.generateTestIndividual()).willReturn(testIndividual)
      given(underTest.testUserRepository.createUser(any[TestUser]())).willReturn(failed(new RuntimeException("test error")))

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

      val testOrgWithHashedPassword = testOrganisation.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testOrgWithHashedPassword)
      verify(underTest.desSimulatorConnector).createOrganisation(testOrgWithHashedPassword)
    }

    "fail when the repository fails" in new Setup {

      given(underTest.generator.generateTestOrganisation()).willReturn(testOrganisation)
      given(underTest.testUserRepository.createUser(any[TestUser]())).willReturn(failed(new RuntimeException("test error")))

      intercept[RuntimeException]{await(underTest.createTestIndividual())}
    }
  }

  "createTestAgent" should {

    "Generate an agent and save it in the database" in new Setup {

      val hashedPassword  = "hashedPassword"
      given(underTest.generator.generateTestAgent(any())).willReturn(testAgent)
      given(underTest.passwordService.hash(testAgent.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestAgent(CreateUserRequest(Some(Seq("some-service")))))

      result shouldBe testAgent
      verify(underTest.testUserRepository).createUser(testAgent.copy(password = hashedPassword))
    }

    "fail when the repository fails" in new Setup {

      given(underTest.generator.generateTestOrganisation()).willReturn(testOrganisation)
      given(underTest.testUserRepository.createUser(any[TestUser]())).willReturn(failed(new RuntimeException("test error")))

      intercept[RuntimeException]{await(underTest.createTestIndividual())}
    }
  }

  val sameUserCreated = new Answer[Future[TestUser]] {
    override def answer(invocationOnMock: InvocationOnMock): Future[TestUser] = {
      successful(invocationOnMock.getArguments.head.asInstanceOf[TestUser])
    }
  }
}
