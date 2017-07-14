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
import org.mockito.Mockito.{times, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar
import play.api.Logger
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.connectors.DesSimulatorConnector
import uk.gov.hmrc.testuser.models.ServiceName.{ServiceName => _, _}
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.services.Generator._
import uk.gov.hmrc.testuser.services.{Generator, PasswordService, TestUserService}
import uk.gov.hmrc.testuser.models.UserNotFound

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class TestUserServiceSpec extends UnitSpec with MockitoSugar with LogSuppressing {

  val userId = "user"
  val password = "password"
  val hashedPassword = "hashedPassword"
  val saUtr = SaUtr("1555369052")
  val nino = Nino("CC333333C")
  val shortNino = NinoNoSuffix("CC333333")

  val individualServices = Seq(ServiceName.NATIONAL_INSURANCE, ServiceName.MTD_INCOME_TAX)
  val testIndividualWithNoServices = generateTestIndividual()
    .copy(
      userId = userId,
      password = password,
      nino = Some(nino),
      saUtr = Some(saUtr)
    )
  val testIndividual = testIndividualWithNoServices.copy(services = individualServices)

  val organisationServices = Seq(ServiceName.NATIONAL_INSURANCE, ServiceName.MTD_INCOME_TAX)
  val testOrganisationWithNoServices = generateTestOrganisation().copy(userId = userId, password = password)
  val testOrganisation = testOrganisationWithNoServices.copy(services = organisationServices)

  val agentServices = Seq(ServiceName.AGENT_SERVICES)
  val testAgent = generateTestAgent(agentServices).copy(userId = userId, password = password)

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

  "fetchIndividualByNino" should {
    "return the individual when it exists in the repository" in new Setup {
      given(underTest.testUserRepository.fetchIndividualByNino(nino)).willReturn(Some(testIndividual))

      val result = await(underTest.fetchIndividualByNino(nino))

      result shouldBe testIndividual
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      given(underTest.testUserRepository.fetchIndividualByNino(nino)).willReturn(None)

      intercept[UserNotFound] {await(underTest.fetchIndividualByNino(nino))}
    }

    "propagate the error when the repository fails" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { suppressedLogs =>
        given(underTest.testUserRepository.fetchIndividualByNino(any())).willReturn(failed(new RuntimeException("expected test error")))
        intercept[RuntimeException] {
          await(underTest.fetchIndividualByNino(nino))
        }
      }
    }
  }

  "fetchIndividualByShortNino" should {
    "return the individual when it exists in the repository" in new Setup {
      given(underTest.testUserRepository.fetchIndividualByShortNino(shortNino)).willReturn(Some(testIndividual))

      val result = await(underTest.fetchIndividualByShortNino(shortNino))

      result shouldBe testIndividual
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      given(underTest.testUserRepository.fetchIndividualByShortNino(shortNino)).willReturn(None)

      intercept[UserNotFound] {await(underTest.fetchIndividualByShortNino(shortNino))}
    }

    "propagate the error when the repository fails" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { suppressedLogs =>
        given(underTest.testUserRepository.fetchIndividualByShortNino(any())).willReturn(failed(new RuntimeException("expected test error")))
        intercept[RuntimeException] {
          await(underTest.fetchIndividualByShortNino(shortNino))
        }
      }
    }
  }

  "fetchIndividualBySaUtr" should {
    "return the individual when it exists in the repository" in new Setup {
      given(underTest.testUserRepository.fetchIndividualBySaUtr(saUtr)).willReturn(Some(testIndividual))

      val result = await(underTest.fetchIndividualBySaUtr(saUtr))

      result shouldBe testIndividual
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      given(underTest.testUserRepository.fetchIndividualBySaUtr(saUtr)).willReturn(None)

      intercept[UserNotFound] {await(underTest.fetchIndividualBySaUtr(saUtr))}
    }

    "propagate the error when the repository fails" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { suppressedLogs =>
        given(underTest.testUserRepository.fetchIndividualBySaUtr(any())).willReturn(failed(new RuntimeException("expected test error")))
        intercept[RuntimeException] {
          await(underTest.fetchIndividualBySaUtr(saUtr))
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
