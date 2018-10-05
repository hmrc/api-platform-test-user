/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.Logger
import uk.gov.hmrc.domain.{EmpRef, Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.testuser.connectors.{AgentsExternalStubsConnector, DesSimulatorConnector}
import uk.gov.hmrc.testuser.models.ServiceName._
import uk.gov.hmrc.testuser.models.UserType.{INDIVIDUAL, ORGANISATION}
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class TestUserService @Inject()(val passwordService: PasswordService,
                                val desSimulatorConnector: DesSimulatorConnector,
                                val testUserRepository: TestUserRepository,
                                val generator: Generator,
                                val agentsExternalStubsConnector: AgentsExternalStubsConnector) {

  def createTestIndividual(serviceNames: Seq[ServiceName])(implicit hc: HeaderCarrier): Future[TestIndividual] = {
    val individual = generator.generateTestIndividual(serviceNames)

    for {
      _ <- createIndividual(individual)
      _ <- syncToAgentExternalStubs(individual)
    } yield individual
  }

  def createTestOrganisation(serviceNames: Seq[ServiceName])(implicit hc: HeaderCarrier): Future[TestOrganisation] = {
    val organisation = generator.generateTestOrganisation(serviceNames)

    for {
      _ <- createOrganisation(organisation)
      _ <- syncToAgentExternalStubs(organisation)
    } yield organisation
  }

  def createTestAgent(serviceNames: Seq[ServiceName])(implicit hc: HeaderCarrier): Future[TestAgent] = {
    val agent = generator.generateTestAgent(serviceNames)
    val hashedPassword = passwordService.hash(agent.password)

    for {
      _ <- testUserRepository.createUser(agent.copy(password = hashedPassword))
      _ <- syncToAgentExternalStubs(agent)
    } yield agent
  }

  private def createIndividual(individual: TestIndividual)(implicit hs: HeaderCarrier): Future[TestIndividual] = for {
    createdIndividual <- testUserRepository.createUser(individual.copy(password = passwordService.hash(individual.password)))
    _ <- if(createdIndividual.services.contains(ServiceName.MTD_INCOME_TAX)) {
            desSimulatorConnector.createIndividual(createdIndividual)
         } else Future.successful(individual)
  } yield individual

  private def createOrganisation(organisation: TestOrganisation)(implicit hs: HeaderCarrier): Future[TestOrganisation] = for {
    createdOrganisation <- testUserRepository.createUser(organisation.copy(password = passwordService.hash(organisation.password)))
    _ <- if(createdOrganisation.services.contains(ServiceName.MTD_INCOME_TAX)) desSimulatorConnector.createOrganisation(createdOrganisation)
    else Future.successful(organisation)
  } yield organisation

  private def syncToAgentExternalStubs(user: TestUser)(implicit hs: HeaderCarrier) =
    agentsExternalStubsConnector.createTestUser(user).recover {
      case NonFatal(e) =>
        Logger.info(s"User ${user.userId} sync to agents-external-stubs failed", e)
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
    case _ => throw UserNotFound(userType)
  }
}
