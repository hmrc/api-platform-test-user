/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.config.ConfigFactory

import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.testuser.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.testuser.connectors.DesSimulatorConnector
import uk.gov.hmrc.testuser.models.ServiceKeys.{ServiceKey => _}
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.services.Generator

class TestUserServiceSpec extends AsyncHmrcSpec {
  implicit def ec = ExecutionContext.global

  val userId          = "user"
  val groupIdentifier = "groupIdentifier"
  val password        = "password"
  val hashedPassword  = "hashedPassword"
  val saUtr           = "1555369052"
  val ctUtr           = "1555369053"
  val crn             = "12345678"
  val nino            = "CC333333C"
  val shortNino       = "CC333333"
  val empRef          = "555/EIA000"

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

  val organisationServices = Seq(ServiceKeys.NATIONAL_INSURANCE, ServiceKeys.MTD_INCOME_TAX)

  val agentServices = Seq(ServiceKeys.AGENT_SERVICES)

  val testAgent = TestAgent(
    userId = userId,
    password = password,
    userFullName = "name",
    emailAddress = "email",
    groupIdentifier = Some(groupIdentifier)
  )

  trait Setup {
    implicit val hc               = HeaderCarrier()
    implicit def executionContext = mock[ExecutionContext]

    val mockTestUserRepository = mock[TestUserRepository]
    val generator              = new Generator(mockTestUserRepository, config)

    val underTest = new TestUserService(mock[PasswordService], mock[DesSimulatorConnector], mockTestUserRepository, mock[Generator])
    when(underTest.testUserRepository.createUser(*[TestUser])).thenAnswer((testUser: TestUser) => successful(testUser))
    when(underTest.testUserRepository.fetchByUserId(*)).thenReturn(successful(None))
    when(underTest.passwordService.validate(*, *)).thenReturn(false)
    when(underTest.passwordService.validate(password, hashedPassword)).thenReturn(true)

    val testIndividualWithNoServices = await(generator.generateTestIndividual(Seq.empty, None, None).map(
      _.copy(
        userId = userId,
        password = password,
        nino = Some(nino),
        saUtr = Some(saUtr)
      )
    ))

    val testIndividual = testIndividualWithNoServices.copy(services = individualServices)

    val testOrganisationWithNoServices = await(generator.generateTestOrganisation(Seq.empty, None, None, None).map(
      _.copy(
        userId = userId,
        password = password,
        empRef = Some(empRef)
      )
    ))

    val testOrganisation = testOrganisationWithNoServices.copy(services = organisationServices)
  }

  "createTestIndividual" should {

    "Generate an individual and save it with hashed password in the database" in new Setup {

      val hashedPassword = "hashedPassword"
      when(underTest.generator.generateTestIndividual(individualServices, None, None)).thenReturn(successful(testIndividual))
      when(underTest.passwordService.hash(testIndividual.password)).thenReturn(hashedPassword)

      val result = await(underTest.createTestIndividual(individualServices))

      result shouldBe Right(testIndividual)

      val testIndividualWithHashedPassword = testIndividual.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testIndividualWithHashedPassword)
      verify(underTest.desSimulatorConnector).createIndividual(testIndividualWithHashedPassword)
    }

    "Not call the DES simulator when the individual does not have the mtd-income-tax service" in new Setup {
      val hashedPassword = "hashedPassword"
      when(underTest.generator.generateTestIndividual(Seq.empty, None, None)).thenReturn(successful(testIndividualWithNoServices))
      when(underTest.passwordService.hash(testIndividualWithNoServices.password)).thenReturn(hashedPassword)

      val result = await(underTest.createTestIndividual(Seq.empty))

      result shouldBe Right(testIndividualWithNoServices)

      val testIndividualWithHashedPassword = testIndividualWithNoServices.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testIndividualWithHashedPassword)
      verify(underTest.desSimulatorConnector, times(0)).createIndividual(testIndividualWithHashedPassword)
    }

    "fail when the repository fails" in new Setup {
      when(underTest.generator.generateTestIndividual(individualServices, None, None)).thenReturn(successful(testIndividual))
      when(underTest.testUserRepository.createUser(*[TestUser]))
        .thenReturn(failed(new RuntimeException("expected test error")))

      intercept[RuntimeException] {
        await(underTest.createTestIndividual(individualServices))
      }
    }

    "fail when the nino validation fails" in new Setup {
      when(underTest.testUserRepository.fetchByNino(eqTo(Nino(nino))))
        .thenReturn(Future.successful(Some(testIndividual)))

      val result = await(underTest.createTestIndividual(individualServices, nino = Some(Nino(nino))))

      result shouldBe Left(NinoAlreadyUsed)

      verify(underTest.testUserRepository, times(0)).createUser(any)
      verify(underTest.desSimulatorConnector, times(0)).createIndividual(any)(any)
    }
  }

  "createTestOrganisation" should {

    "Generate an organisation and save it in the database" in new Setup {

      val hashedPassword = "hashedPassword"
      when(underTest.generator.generateTestOrganisation(organisationServices, None, None, None)).thenReturn(successful(testOrganisation))
      when(underTest.passwordService.hash(testOrganisation.password)).thenReturn(hashedPassword)

      val result = await(underTest.createTestOrganisation(organisationServices, None, None, None))

      result shouldBe Right(testOrganisation)

      val testOrgWithHashedPassword = testOrganisation.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testOrgWithHashedPassword)
      verify(underTest.desSimulatorConnector).createOrganisation(testOrgWithHashedPassword)
    }

    "Not call the DES simulator when the organisation does not have the mtd-income-tax service" in new Setup {

      val hashedPassword = "hashedPassword"
      when(underTest.generator.generateTestOrganisation(Seq.empty, None, None, None)).thenReturn(successful(testOrganisationWithNoServices))
      when(underTest.passwordService.hash(testOrganisationWithNoServices.password)).thenReturn(hashedPassword)

      val result = await(underTest.createTestOrganisation(Seq.empty, None, None, None))

      result shouldBe Right(testOrganisationWithNoServices)

      val testOrgWithHashedPassword = testOrganisationWithNoServices.copy(password = hashedPassword)
      verify(underTest.testUserRepository).createUser(testOrgWithHashedPassword)
      verify(underTest.desSimulatorConnector, times(0)).createOrganisation(testOrgWithHashedPassword)
    }

    "fail when the repository fails" in new Setup {
      when(underTest.generator.generateTestOrganisation(organisationServices, None, None, None)).thenReturn(successful(testOrganisation))
      when(underTest.testUserRepository.createUser(*[TestUser]))
        .thenReturn(failed(new RuntimeException("expected test error")))

      intercept[RuntimeException] {
        await(underTest.createTestOrganisation(organisationServices, None, None, None))
      }
    }

    "fail when the nino validation fails" in new Setup {
      when(underTest.testUserRepository.fetchByNino(eqTo(Nino(nino))))
        .thenReturn(Future.successful(Some(testIndividual)))

      val result = await(underTest.createTestOrganisation(organisationServices, None, Some(Nino(nino)), None))

      result shouldBe Left(NinoAlreadyUsed)

      verify(underTest.testUserRepository, times(0)).createUser(any)
      verify(underTest.desSimulatorConnector, times(0)).createIndividual(any)(any)
    }
  }

  "createTestAgent" should {

    "Generate an agent and save it in the database" in new Setup {

      val hashedPassword = "hashedPassword"
      when(underTest.generator.generateTestAgent(agentServices)).thenReturn(successful(testAgent))
      when(underTest.passwordService.hash(testAgent.password)).thenReturn(hashedPassword)

      val result = await(underTest.createTestAgent(agentServices))

      result shouldBe testAgent
      verify(underTest.testUserRepository).createUser(testAgent.copy(password = hashedPassword))
    }

    "fail when the repository fails" in new Setup {
      when(underTest.generator.generateTestAgent(*)).thenReturn(successful(testAgent))
      when(underTest.testUserRepository.createUser(*[TestUser]))
        .thenReturn(failed(new RuntimeException("expected test error")))

      intercept[RuntimeException] {
        await(underTest.createTestAgent(agentServices))
      }
    }
  }

  "fetchIndividualByNino" should {
    "return the individual when it exists in the repository" in new Setup {
      when(underTest.testUserRepository.fetchIndividualByNino(Nino(nino))).thenReturn(successful(Some(testIndividual)))

      val result = await(underTest.fetchIndividualByNino(Nino(nino)))

      result shouldBe testIndividual
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      when(underTest.testUserRepository.fetchIndividualByNino(Nino(nino))).thenReturn(successful(None))

      intercept[UserNotFound] {
        await(underTest.fetchIndividualByNino(Nino(nino)))
      }
    }

    "propagate the error when the repository fails" in new Setup {
      when(underTest.testUserRepository.fetchIndividualByNino(*)).thenReturn(failed(new RuntimeException("expected test error")))
      intercept[RuntimeException] {
        await(underTest.fetchIndividualByNino(Nino(nino)))
      }
    }
  }

  "fetchIndividualByShortNino" should {
    "return the individual when it exists in the repository" in new Setup {
      when(underTest.testUserRepository.fetchIndividualByShortNino(NinoNoSuffix(shortNino))).thenReturn(successful(Some(testIndividual)))

      val result = await(underTest.fetchIndividualByShortNino(NinoNoSuffix(shortNino)))

      result shouldBe testIndividual
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      when(underTest.testUserRepository.fetchIndividualByShortNino(NinoNoSuffix(shortNino))).thenReturn(successful(None))

      intercept[UserNotFound] {
        await(underTest.fetchIndividualByShortNino(NinoNoSuffix(shortNino)))
      }
    }

    "propagate the error when the repository fails" in new Setup {
      when(underTest.testUserRepository.fetchIndividualByShortNino(*)).thenReturn(failed(new RuntimeException("expected test error")))
      intercept[RuntimeException] {
        await(underTest.fetchIndividualByShortNino(NinoNoSuffix(shortNino)))
      }
    }
  }

  "fetchIndividualBySaUtr" should {
    "return the individual when it exists in the repository" in new Setup {
      when(underTest.testUserRepository.fetchIndividualBySaUtr(SaUtr(saUtr))).thenReturn(successful(Some(testIndividual)))

      val result = await(underTest.fetchIndividualBySaUtr(SaUtr(saUtr)))

      result shouldBe testIndividual
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      when(underTest.testUserRepository.fetchIndividualBySaUtr(SaUtr(saUtr))).thenReturn(successful(None))

      intercept[UserNotFound] {
        await(underTest.fetchIndividualBySaUtr(SaUtr(saUtr)))
      }
    }

    "propagate the error when the repository fails" in new Setup {
      when(underTest.testUserRepository.fetchIndividualBySaUtr(*)).thenReturn(failed(new RuntimeException("expected test error")))
      intercept[RuntimeException] {
        await(underTest.fetchIndividualBySaUtr(SaUtr(saUtr)))
      }
    }
  }

  "fetchOrganisationByCtUtr" should {
    "return the organisation when it exists in the repository" in new Setup {
      when(underTest.testUserRepository.fetchOrganisationByCtUtr(CtUtr(ctUtr))).thenReturn(successful(Some(testOrganisation)))

      val result = await(underTest.fetchOrganisationByCtUtr(CtUtr(ctUtr)))

      result shouldBe testOrganisation
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      when(underTest.testUserRepository.fetchOrganisationByCtUtr(CtUtr(ctUtr))).thenReturn(successful(None))

      intercept[UserNotFound] {
        await(underTest.fetchOrganisationByCtUtr(CtUtr(ctUtr)))
      }
    }

    "propagate the error when the repository fails" in new Setup {
      when(underTest.testUserRepository.fetchOrganisationByCtUtr(*)).thenReturn(failed(new RuntimeException("expected test error")))
      intercept[RuntimeException] {
        await(underTest.fetchOrganisationByCtUtr(CtUtr(ctUtr)))
      }
    }
  }

  "fetchOrganisationBySaUtr" should {
    "return the organisation when it exists in the repository" in new Setup {
      when(underTest.testUserRepository.fetchOrganisationBySaUtr(SaUtr(saUtr))).thenReturn(successful(Some(testOrganisation)))

      val result = await(underTest.fetchOrganisationBySaUtr(SaUtr(saUtr)))

      result shouldBe testOrganisation
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      when(underTest.testUserRepository.fetchOrganisationBySaUtr(SaUtr(saUtr))).thenReturn(successful(None))

      intercept[UserNotFound] {
        await(underTest.fetchOrganisationBySaUtr(SaUtr(saUtr)))
      }
    }

    "propagate the error when the repository fails" in new Setup {
      when(underTest.testUserRepository.fetchOrganisationBySaUtr(*)).thenReturn(failed(new RuntimeException("expected test error")))
      intercept[RuntimeException] {
        await(underTest.fetchOrganisationBySaUtr(SaUtr(saUtr)))
      }
    }
  }

  "fetchOrganisationByCrn" should {
    "return the organisation when it exists in the repository" in new Setup {
      when(underTest.testUserRepository.fetchOrganisationByCrn(Crn(crn))).thenReturn(successful(Some(testOrganisation)))

      val result = await(underTest.fetchOrganisationByCrn(Crn(crn)))

      result shouldBe testOrganisation
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      when(underTest.testUserRepository.fetchOrganisationByCrn(Crn(crn))).thenReturn(successful(None))

      intercept[UserNotFound] {
        await(underTest.fetchOrganisationByCrn(Crn(crn)))
      }
    }

    "propagate the error when the repository fails" in new Setup {
      when(underTest.testUserRepository.fetchOrganisationByCrn(*)).thenReturn(failed(new RuntimeException("expected test error")))
      intercept[RuntimeException] {
        await(underTest.fetchOrganisationByCrn(Crn(crn)))
      }
    }
  }

  "fetchOrganisationByEmpRef" should {
    "return the organisation when it exists in the repository" in new Setup {
      when(underTest.testUserRepository.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef))).thenReturn(successful(Some(testOrganisation)))

      val result = await(underTest.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef)))

      result shouldBe testOrganisation
    }

    "fail with UserNotFound when the individual does not exist in the repository" in new Setup {
      when(underTest.testUserRepository.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef))).thenReturn(successful(None))

      intercept[UserNotFound] {
        await(underTest.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef)))
      }
    }

    "propagate the error when the repository fails" in new Setup {
      when(underTest.testUserRepository.fetchOrganisationByEmpRef(*)).thenReturn(failed(new RuntimeException("expected test error")))
      intercept[RuntimeException] {
        await(underTest.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef)))
      }
    }
  }
}
