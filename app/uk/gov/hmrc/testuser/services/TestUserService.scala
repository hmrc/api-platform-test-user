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

package uk.gov.hmrc.testuser.services

import javax.inject.Inject

import uk.gov.hmrc.domain.{EmpRef, SaUtr, Nino}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.testuser.connectors.{DesSimulatorConnector, DesSimulatorConnectorImpl}
import uk.gov.hmrc.testuser.models.UserType.{ORGANISATION, INDIVIDUAL}
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.LegacySandboxUser._
import uk.gov.hmrc.testuser.models.ServiceName._
import uk.gov.hmrc.testuser.repository.TestUserRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

trait TestUserService {

  val generator: Generator
  val testUserRepository: TestUserRepository
  val passwordService: PasswordService
  val desSimulatorConnector: DesSimulatorConnector

  def createTestIndividual(serviceNames: Seq[ServiceName])(implicit hc: HeaderCarrier) = {
    val individual = generator.generateTestIndividual(serviceNames)
    val hashedPassword = passwordService.hash(individual.password)

    testUserRepository.createUser(individual.copy(password = hashedPassword)) map {
      case createdIndividual if createdIndividual.services.contains(ServiceName.MTD_INCOME_TAX) => desSimulatorConnector.createIndividual(createdIndividual)
      case _ => Future.successful(individual)
    } map {
      _ => individual
    }
  }

  def createTestOrganisation(serviceNames: Seq[ServiceName])(implicit hc: HeaderCarrier) = {
    val organisation = generator.generateTestOrganisation(serviceNames)
    val hashedPassword = passwordService.hash(organisation.password)
    testUserRepository.createUser(organisation.copy(password = hashedPassword)) map {
      case createdOrganisation if createdOrganisation.services.contains(ServiceName.MTD_INCOME_TAX) => desSimulatorConnector.createOrganisation(createdOrganisation)
      case _ => Future.successful(organisation)
    } map {
      _ => organisation
    }
  }

  def createTestAgent(serviceNames: Seq[ServiceName]) = {
    val agent = generator.generateTestAgent(serviceNames)
    val hashedPassword = passwordService.hash(agent.password)
    testUserRepository.createUser(agent.copy(password = hashedPassword)) map (_ => agent)
  }

  def fetchIndividualByNino(nino: Nino)(implicit hc: HeaderCarrier): Future[TestIndividual] = {
    testUserRepository.fetchIndividualByNino(nino) map getOrFailWithUserNotFound(INDIVIDUAL)
  }

  def fetchIndividualByShortNino(shortNino: NinoNoSuffix)(implicit hc: HeaderCarrier): Future[TestIndividual] = {
    testUserRepository.fetchIndividualByShortNino(shortNino) map getOrFailWithUserNotFound(INDIVIDUAL)
  }

  def fetchIndividualBySaUtr(saUtr: SaUtr)(implicit hc: HeaderCarrier): Future[TestIndividual] = {
    testUserRepository.fetchIndividualBySaUtr(saUtr) map getOrFailWithUserNotFound(INDIVIDUAL)
  }

  def fetchOrganisationByEmpRef(empRef: EmpRef)(implicit hc: HeaderCarrier): Future[TestOrganisation] = {
    testUserRepository.fetchOrganisationByEmpRef(empRef) map getOrFailWithUserNotFound(ORGANISATION)
  }

  def getOrFailWithUserNotFound[T <: TestUser](userType: UserType.Value) = PartialFunction[Option[T], T] {
    case Some(t) => t
    case _ => throw new UserNotFound(userType)
  }
}


class TestUserServiceImpl @Inject()(override val passwordService: PasswordServiceImpl,
                                    override val desSimulatorConnector: DesSimulatorConnectorImpl) extends TestUserService {
  override val generator: Generator = Generator
  override val testUserRepository = TestUserRepository()
}
