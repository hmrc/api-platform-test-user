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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.domain.{CtUtr, EmpRef, Nino, SaUtr, Vrn}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.testuser.connectors.DesSimulatorConnector
import uk.gov.hmrc.testuser.models.ServiceKey._
import uk.gov.hmrc.testuser.models.UserType.{INDIVIDUAL, ORGANISATION}
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserRepository

trait CreateTestUserError
object NinoAlreadyUsed      extends CreateTestUserError
object Pillar2IdAlreadyUsed extends CreateTestUserError

@Singleton
class TestUserService @Inject() (
    val passwordService: PasswordService,
    val desSimulatorConnector: DesSimulatorConnector,
    val testUserRepository: TestUserRepository,
    val generator: Generator
  )(implicit ec: ExecutionContext
  ) {

  private def validateField[A, T](
      maybeValue: Option[A],
      isUnique: A => Future[Boolean],
      error: CreateTestUserError,
      allowDuplicate: A => Boolean = (_: A) => false
    )(createTestUser: => Future[T]): Future[Either[CreateTestUserError, T]] = {
    maybeValue
      .fold(Future.successful(true))(value => 
        if (allowDuplicate(value)) Future.successful(true)
        else isUnique(value)
      )
      .flatMap(isValid => {
        if (isValid) createTestUser.map(Right(_))
        else Future.successful(Left(error))
      })
  }

  private def validateNinoRequest[T](maybeNino: Option[Nino])(createTestUser: => Future[T]): Future[Either[CreateTestUserError, T]] = {
    validateField(
      maybeValue = maybeNino,
      isUnique = (nino: Nino) => testUserRepository.fetchByNino(nino).map(_.fold(true)(_ => false)),
      error = NinoAlreadyUsed
    )(createTestUser)
  }

  private def validatePillar2IdRequest[T](maybePillar2Id: Option[Pillar2Id])(createTestUser: => Future[T]): Future[Either[CreateTestUserError, T]] = {
    validateField(
      maybeValue = maybePillar2Id,
      isUnique = (pillar2Id: Pillar2Id) => testUserRepository.fetchOrganisationByPillar2Id(pillar2Id).map(_.fold(true)(_ => false)),
      error = Pillar2IdAlreadyUsed,
      allowDuplicate = (pillar2Id: Pillar2Id) => AllowedDuplicatePillar2Ids.values.contains(pillar2Id.value)
    )(createTestUser)
  }

  def createTestIndividual(
      serviceNames: Seq[ServiceKey],
      eoriNumber: Option[EoriNumber] = None,
      nino: Option[Nino] = None
    )(implicit hc: HeaderCarrier
    ): Future[Either[CreateTestUserError, TestIndividual]] = validateNinoRequest(nino) {
    generator.generateTestIndividual(serviceNames, eoriNumber, nino).flatMap { individual =>
      val hashedPassword = passwordService.hash(individual.password)

      testUserRepository.createUser(individual.copy(password = hashedPassword)) map {
        case createdIndividual if createdIndividual.services.contains(MTD_INCOME_TAX) =>
          desSimulatorConnector.createIndividual(createdIndividual)
        case _                                                                        => individual
      } map {
        _ => individual
      }
    }

  }

  def createTestOrganisation(
      serviceNames: Seq[ServiceKey],
      eoriNumber: Option[EoriNumber],
      exciseNumber: Option[ExciseNumber],
      nino: Option[Nino],
      taxpayerType: Option[TaxpayerType],
      pillar2Id: Option[Pillar2Id]
    )(implicit hc: HeaderCarrier
    ): Future[Either[CreateTestUserError, TestOrganisation]] = {

    def createOrg = generator.generateTestOrganisation(serviceNames, eoriNumber, exciseNumber, nino, taxpayerType, pillar2Id).flatMap { organisation =>
      val hashedPassword = passwordService.hash(organisation.password)

      testUserRepository.createUser(organisation.copy(password = hashedPassword)) map {
        case createdOrganisation if createdOrganisation.services.contains(MTD_INCOME_TAX) =>
          desSimulatorConnector.createOrganisation(createdOrganisation)
        case _                                                                            => organisation
      } map {
        _ => organisation
      }
    }

    (nino, pillar2Id) match {
      case (Some(n), None) =>
        validateNinoRequest(Some(n)) {
          createOrg
        }

      case (None, Some(p)) =>
        validatePillar2IdRequest(Some(p)) {
          createOrg
        }

      case (None, None) => // Neither provided
        createOrg.map(Right(_))

      case (Some(_), Some(_)) =>
        Future.successful(Left(NinoAlreadyUsed))
    }
  }

  def createTestAgent(serviceNames: Seq[ServiceKey]) = {
    generator.generateTestAgent(serviceNames).flatMap { agent =>
      val hashedPassword = passwordService.hash(agent.password)
      testUserRepository.createUser(agent.copy(password = hashedPassword)) map (_ => agent)
    }
  }

  def fetchIndividualByNino(nino: Nino): Future[TestIndividual] = {
    testUserRepository.fetchIndividualByNino(nino) map (t => t.getOrElse(throw UserNotFound(INDIVIDUAL)))
  }

  def fetchIndividualByShortNino(shortNino: NinoNoSuffix): Future[TestIndividual] = {
    testUserRepository.fetchIndividualByShortNino(shortNino) map (t => t.getOrElse(throw UserNotFound(INDIVIDUAL)))
  }

  def fetchIndividualBySaUtr(saUtr: SaUtr): Future[TestIndividual] = {
    testUserRepository.fetchIndividualBySaUtr(saUtr) map (t => t.getOrElse(throw UserNotFound(INDIVIDUAL)))
  }

  def fetchIndividualByVrn(vrn: Vrn): Future[TestIndividual] = {
    testUserRepository.fetchIndividualByVrn(vrn) map (t => t.getOrElse(throw UserNotFound(INDIVIDUAL)))
  }

  def fetchOrganisationByEmpRef(empRef: EmpRef): Future[TestOrganisation] = {
    testUserRepository.fetchOrganisationByEmpRef(empRef) map (t => t.getOrElse(throw UserNotFound(ORGANISATION)))
  }

  def fetchOrganisationByVrn(vrn: Vrn): Future[TestOrganisation] = {
    testUserRepository.fetchOrganisationByVrn(vrn) map (t => t.getOrElse(throw UserNotFound(ORGANISATION)))
  }

  def fetchOrganisationByCtUtr(utr: CtUtr): Future[TestOrganisation] = {
    testUserRepository.fetchOrganisationByCtUtr(utr) map (t => t.getOrElse(throw UserNotFound(ORGANISATION)))
  }

  def fetchOrganisationBySaUtr(saUtr: SaUtr): Future[TestOrganisation] = {
    testUserRepository.fetchOrganisationBySaUtr(saUtr) map (t => t.getOrElse(throw UserNotFound(ORGANISATION)))
  }

  def fetchOrganisationByCrn(crn: Crn): Future[TestOrganisation] = {
    testUserRepository.fetchOrganisationByCrn(crn) map (t => t.getOrElse(throw UserNotFound(ORGANISATION)))
  }

  def fetchOrganisationByPillar2Id(pillar2Id: Pillar2Id): Future[TestOrganisation] = {
    testUserRepository.fetchOrganisationByPillar2Id(pillar2Id) map (t => t.getOrElse(throw UserNotFound(ORGANISATION)))
  }
}
