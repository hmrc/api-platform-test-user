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

package uk.gov.hmrc.testuser.services

import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.BDDMockito.given
import org.mockito.Mockito.{times, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar
import play.api.Logger
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.common.LogSuppressing
import uk.gov.hmrc.testuser.connectors.DesSimulatorConnector
import uk.gov.hmrc.testuser.models.ServiceKeys.{ServiceKey => _}
import uk.gov.hmrc.testuser.models.{UserNotFound, _}
import uk.gov.hmrc.testuser.repository.TestUserRepository

import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

class TestUserServiceSpec extends UnitSpec with MockitoSugar with LogSuppressing {
  implicit def ec = ExecutionContext.global

  val mockTestUserRepository = mock[TestUserRepository]

  val userId = "user"
  val groupIdentifier = "groupIdentifier"
  val password = "password"
  val hashedPassword = "hashedPassword"
  val saUtr = "1555369052"
  val nino = "CC333333C"
  val shortNino = "CC333333"
  val empRef = "555/EIA000"

  val individualServices = Seq(ServiceKeys.NATIONAL_INSURANCE, ServiceKeys.MTD_INCOME_TAX)
  val config = ConfigFactory.parseString(
    """randomiser {
      |  individualDetails {
      |    firstName = [
      |      "Adrian"
      |    ]
      |
      |    lastName = [
      |      "Adams"
      |    ]
      |
      |    dateOfBirth = [
      |      "1940-10-10"
      |    ]
      |  }
      |
      |  address {
      |    line1 = [
      |      "1 Abbey Road"
      |    ]
      |
      |    line2 = [
      |      "Aberdeen"
      |    ]
      |
      |    postcode = [
      |      "TS1 1PA"
      |    ]
      |  }
      |}
      |""".stripMargin
  )

  val generator = new Generator(mockTestUserRepository, config)
  val testIndividualWithNoServices = generator.generateTestIndividual(Seq.empty, None)
    .copy(
      userId = userId,
      password = password,
      nino = Some(nino),
      saUtr = Some(saUtr)
    )
  val testIndividual = testIndividualWithNoServices.copy(services = individualServices)

  val organisationServices = Seq(ServiceKeys.NATIONAL_INSURANCE, ServiceKeys.MTD_INCOME_TAX)
  val testOrganisationWithNoServices = generator.generateTestOrganisation(Seq.empty, None)
    .copy(
      userId = userId,
      password = password,
      empRef = Some(empRef))
  val testOrganisation = testOrganisationWithNoServices.copy(services = organisationServices)

  val agentServices = Seq(ServiceKeys.AGENT_SERVICES)
  val testAgent = TestAgent(
    userId = userId,
    password = password,
    userFullName = "name",
    emailAddress = "email",
    groupIdentifier = Some(groupIdentifier)
  )

  trait Setup {
    implicit val hc = HeaderCarrier()
    implicit def executionContext = mock[ExecutionContext]

    val underTest = new TestUserService(mock[PasswordService], mock[DesSimulatorConnector], mockTestUserRepository, mock[Generator])
    when(underTest.testUserRepository.createUser(any[TestUser]())).thenAnswer(sameUserCreated)
    when(underTest.testUserRepository.fetchByUserId(anyString())).thenReturn(successful(None))
    when(underTest.passwordService.validate(anyString(), anyString())).thenReturn(false)
    when(underTest.passwordService.validate(password, hashedPassword)).thenReturn(true)
  }

  "createTestIndividual" should {

    "Generate an individual and save it with hashed password in the database" in new Setup {

      val hashedPassword = "hashedPassword"
      given(underTest.generator.generateTestIndividual(individualServices, None)).willReturn(testIndividual)
      given(underTest.passwordService.hash(testIndividual.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestIndividual(individualServices))

      result shouldBe testIndividual

      val testIndividualWithHashedPassword = testIndividual.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testIndividualWithHashedPassword)
      verify(underTest.desSimulatorConnector).createIndividual(testIndividualWithHashedPassword)
    }

    "Not call the DES simulator when the individual does not have the mtd-income-tax service" in new Setup {
      val hashedPassword = "hashedPassword"
      given(underTest.generator.generateTestIndividual(Seq.empty, None)).willReturn(testIndividualWithNoServices)
      given(underTest.passwordService.hash(testIndividualWithNoServices.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestIndividual(Seq.empty))

      result shouldBe testIndividualWithNoServices

      val testIndividualWithHashedPassword = testIndividualWithNoServices.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testIndividualWithHashedPassword)
      verify(underTest.desSimulatorConnector, times(0)).createIndividual(testIndividualWithHashedPassword)
    }

    "fail when the repository fails" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { suppressedLogs =>
        given(underTest.generator.generateTestIndividual(individualServices, None)).willReturn(testIndividual)
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

      val hashedPassword = "hashedPassword"
      given(underTest.generator.generateTestOrganisation(organisationServices, None)).willReturn(testOrganisation)
      given(underTest.passwordService.hash(testOrganisation.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestOrganisation(organisationServices, None))

      result shouldBe testOrganisation

      val testOrgWithHashedPassword = testOrganisation.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testOrgWithHashedPassword)
      verify(underTest.desSimulatorConnector).createOrganisation(testOrgWithHashedPassword)
    }

    "Not call the DES simulator when the organisation does not have the mtd-income-tax service" in new Setup {

      val hashedPassword = "hashedPassword"
      given(underTest.generator.generateTestOrganisation(Seq.empty, None)).willReturn(testOrganisationWithNoServices)
      given(underTest.passwordService.hash(testOrganisationWithNoServices.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestOrganisation(Seq.empty, None))

      result shouldBe testOrganisationWithNoServices

      val testOrgWithHashedPassword = testOrganisationWithNoServices.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testOrgWithHashedPassword)
      verify(underTest.desSimulatorConnector, times(0)).createOrganisation(testOrgWithHashedPassword)
    }

    "fail when the repository fails" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { suppressedLogs =>
        given(underTest.generator.generateTestOrganisation(organisationServices, None)).willReturn(testOrganisation)
        given(underTest.testUserRepository.createUser(any[TestUser]()))
          .willReturn(failed(new RuntimeException("expected test error")))

        intercept[RuntimeException] {
          await(underTest.createTestOrganisation(organisationServices, None))
        }
      }
    }
  }

  "createTestAgent" should {

    "Generate an agent and save it in the database" in new Setup {

      val hashedPassword = "hashedPassword"
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
      given(underTest.testUserRepository.fetchIndividualByNino(Nino(nino))).willReturn(Some(testIndividual))

      val result = await(underTest.fetchIndividualByNino(Nino(nino)))

      result shouldBe testIndividual
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      given(underTest.testUserRepository.fetchIndividualByNino(Nino(nino))).willReturn(None)

      intercept[UserNotFound] {
        await(underTest.fetchIndividualByNino(Nino(nino)))
      }
    }

    "propagate the error when the repository fails" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { suppressedLogs =>
        given(underTest.testUserRepository.fetchIndividualByNino(any())).willReturn(failed(new RuntimeException("expected test error")))
        intercept[RuntimeException] {
          await(underTest.fetchIndividualByNino(Nino(nino)))
        }
      }
    }
  }

  "fetchIndividualByShortNino" should {
    "return the individual when it exists in the repository" in new Setup {
      given(underTest.testUserRepository.fetchIndividualByShortNino(NinoNoSuffix(shortNino))).willReturn(Some(testIndividual))

      val result = await(underTest.fetchIndividualByShortNino(NinoNoSuffix(shortNino)))

      result shouldBe testIndividual
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      given(underTest.testUserRepository.fetchIndividualByShortNino(NinoNoSuffix(shortNino))).willReturn(None)

      intercept[UserNotFound] {
        await(underTest.fetchIndividualByShortNino(NinoNoSuffix(shortNino)))
      }
    }

    "propagate the error when the repository fails" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { suppressedLogs =>
        given(underTest.testUserRepository.fetchIndividualByShortNino(any())).willReturn(failed(new RuntimeException("expected test error")))
        intercept[RuntimeException] {
          await(underTest.fetchIndividualByShortNino(NinoNoSuffix(shortNino)))
        }
      }
    }
  }

  "fetchIndividualBySaUtr" should {
    "return the individual when it exists in the repository" in new Setup {
      given(underTest.testUserRepository.fetchIndividualBySaUtr(SaUtr(saUtr))).willReturn(Some(testIndividual))

      val result = await(underTest.fetchIndividualBySaUtr(SaUtr(saUtr)))

      result shouldBe testIndividual
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      given(underTest.testUserRepository.fetchIndividualBySaUtr(SaUtr(saUtr))).willReturn(None)

      intercept[UserNotFound] {
        await(underTest.fetchIndividualBySaUtr(SaUtr(saUtr)))
      }
    }

    "propagate the error when the repository fails" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { suppressedLogs =>
        given(underTest.testUserRepository.fetchIndividualBySaUtr(any())).willReturn(failed(new RuntimeException("expected test error")))
        intercept[RuntimeException] {
          await(underTest.fetchIndividualBySaUtr(SaUtr(saUtr)))
        }
      }
    }
  }

  "fetchOrganisationByEmpRef" should {
    "return the organisation when it exists in the repository" in new Setup {
      given(underTest.testUserRepository.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef))).willReturn(Some(testOrganisation))

      val result = await(underTest.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef)))

      result shouldBe testOrganisation
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      given(underTest.testUserRepository.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef))).willReturn(None)

      intercept[UserNotFound] {
        await(underTest.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef)))
      }
    }

    "propagate the error when the repository fails" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { suppressedLogs =>
        given(underTest.testUserRepository.fetchOrganisationByEmpRef(any())).willReturn(failed(new RuntimeException("expected test error")))
        intercept[RuntimeException] {
          await(underTest.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef)))
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
