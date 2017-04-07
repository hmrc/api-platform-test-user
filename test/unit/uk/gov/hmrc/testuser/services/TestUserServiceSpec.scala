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

import common.LogSuppressing
import org.mockito.BDDMockito.given
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.{verify, when, times}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar
import play.api.Logger
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.connectors.DesSimulatorConnector
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.services.{Generator, PasswordService, TestUserService}

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class TestUserServiceSpec extends UnitSpec with MockitoSugar with LogSuppressing {

  val userId = "user"
  val password = "password"
  val hashedPassword = "hashedPassword"

  val individualServices = Seq(ServiceName.NATIONAL_INSURANCE, ServiceName.MTD_INCOME_TAX)
  val testIndividualWithNoServices = TestIndividual(userId, password, SaUtr("1555369052"), Nino("CC333333C"),
    MtdItId("XGIT00000000054"))
  val testIndividual = testIndividualWithNoServices.copy(services = individualServices)

  val organisationServices = Seq(ServiceName.NATIONAL_INSURANCE, ServiceName.MTD_INCOME_TAX)
  val testOrganisationWithNoServices = TestOrganisation(userId, password, SaUtr("1555369052"), Nino("CC333333C"),
    MtdItId("XGIT00000000054"), EmpRef("555","EIA000"), CtUtr("1555369053"), Vrn("999902541"))
  val testOrganisation = testOrganisationWithNoServices.copy(services = organisationServices)

  val agentServices = Seq(ServiceName.AGENT_SERVICES)
  val testAgent = TestAgent(userId, password, AgentBusinessUtr("NARN0396245"), agentServices)

  val authSession = AuthSession("Bearer AUTH_TOKEN", "/auth/oid/12345", "gatewayToken")
  val storedTestIndividual = TestIndividual(userId, hashedPassword, SaUtr("1555369052"), Nino("CC333333C"), MtdItId("XGIT00000000054"))

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
      given(underTest.generator.generateTestIndividual(individualServices)).willReturn(testIndividual)
      given(underTest.passwordService.hash(testIndividual.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestIndividual(individualServices))

      result shouldBe testIndividual

      val testIndividualWithHashedPassword = testIndividual.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testIndividualWithHashedPassword)
      verify(underTest.desSimulatorConnector).createIndividual(testIndividualWithHashedPassword)
    }

    "Not call the DES simulator when the individual does not have the mtd-income-tax service" in new Setup {
      val hashedPassword  = "hashedPassword"
      given(underTest.generator.generateTestIndividual(Seq.empty)).willReturn(testIndividualWithNoServices)
      given(underTest.passwordService.hash(testIndividualWithNoServices.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestIndividual(Seq.empty))

      result shouldBe testIndividualWithNoServices

      val testIndividualWithHashedPassword = testIndividualWithNoServices.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testIndividualWithHashedPassword)
      verify(underTest.desSimulatorConnector, times(0)).createIndividual(testIndividualWithHashedPassword)
    }

    "fail when the repository fails" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { suppressedLogs =>
        given(underTest.generator.generateTestIndividual(individualServices)).willReturn(testIndividual)
        given(underTest.testUserRepository.createUser(any[TestUser]()))
          .willReturn(failed(new RuntimeException("expected test error")))

        intercept[RuntimeException] {
          await(underTest.createTestIndividual(individualServices))
        }
      }
    }
  }

  "createTestOrganisation" should {

    "Generate an organisation and save it in the database" in new Setup {

      val hashedPassword  = "hashedPassword"
      given(underTest.generator.generateTestOrganisation(organisationServices)).willReturn(testOrganisation)
      given(underTest.passwordService.hash(testOrganisation.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestOrganisation(organisationServices))

      result shouldBe testOrganisation

      val testOrgWithHashedPassword = testOrganisation.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testOrgWithHashedPassword)
      verify(underTest.desSimulatorConnector).createOrganisation(testOrgWithHashedPassword)
    }

    "Not call the DES simulator when the organisation does not have the mtd-income-tax service" in new Setup {

      val hashedPassword  = "hashedPassword"
      given(underTest.generator.generateTestOrganisation(Seq.empty)).willReturn(testOrganisationWithNoServices)
      given(underTest.passwordService.hash(testOrganisationWithNoServices.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestOrganisation(Seq.empty))

      result shouldBe testOrganisationWithNoServices

      val testOrgWithHashedPassword = testOrganisationWithNoServices.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testOrgWithHashedPassword)
      verify(underTest.desSimulatorConnector, times(0)).createOrganisation(testOrgWithHashedPassword)
    }

    "fail when the repository fails" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { suppressedLogs =>
        given(underTest.generator.generateTestOrganisation(organisationServices)).willReturn(testOrganisation)
        given(underTest.testUserRepository.createUser(any[TestUser]()))
          .willReturn(failed(new RuntimeException("expected test error")))

        intercept[RuntimeException] {
          await(underTest.createTestOrganisation(organisationServices))
        }
      }
    }
  }

  "createTestAgent" should {

    "Generate an agent and save it in the database" in new Setup {

      val hashedPassword  = "hashedPassword"
      given(underTest.generator.generateTestAgent(agentServices)).willReturn(testAgent)
      given(underTest.passwordService.hash(testAgent.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestAgent(agentServices))

      result shouldBe testAgent
      verify(underTest.testUserRepository).createUser(testAgent.copy(password = hashedPassword))
    }

    "fail when the repository fails" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { suppressedLogs =>
        given(underTest.generator.generateTestAgent(any())).willReturn(testAgent)
        given(underTest.testUserRepository.createUser(any[TestUser]()))
          .willReturn(failed(new RuntimeException("expected test error")))

        intercept[RuntimeException] {
          await(underTest.createTestAgent(agentServices))
        }
      }
    }
  }

  val sameUserCreated = new Answer[Future[TestUser]] {
    override def answer(invocationOnMock: InvocationOnMock): Future[TestUser] = {
      successful(invocationOnMock.getArguments.head.asInstanceOf[TestUser])
    }
  }
}
