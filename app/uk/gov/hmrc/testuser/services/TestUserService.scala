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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.domain.{CtUtr, EmpRef, Nino, SaUtr, Vrn}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.testuser.connectors.DesSimulatorConnector
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.ServiceKeys._
import uk.gov.hmrc.testuser.models.UserType.{INDIVIDUAL, ORGANISATION}
import uk.gov.hmrc.testuser.repository.TestUserRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestUserService @Inject()(val passwordService: PasswordService,
                                val desSimulatorConnector: DesSimulatorConnector,
                                val testUserRepository: TestUserRepository,
                                val generator: Generator)
                               (implicit ec: ExecutionContext) {

  def createTestIndividual(serviceNames: Seq[ServiceKey], eoriNumber: Option[EoriNumber] = None, nino: Option[String] = None)
                          (implicit hc: HeaderCarrier): Future[TestIndividual] = {
    generator.generateTestIndividual(serviceNames, eoriNumber, nino).flatMap { individual =>
      val hashedPassword = passwordService.hash(individual.password)

      testUserRepository.createUser(individual.copy(password = hashedPassword)) map {
        case createdIndividual if createdIndividual.services.contains(ServiceKeys.MTD_INCOME_TAX) => desSimulatorConnector.createIndividual(createdIndividual)
        case _ => Future.successful(individual)
      } map {
        _ => individual
      }
    }
  }

  def createTestOrganisation(serviceNames: Seq[ServiceKey], eoriNumber: Option[EoriNumber], taxpayerType: Option[TaxpayerType])
                            (implicit hc: HeaderCarrier): Future[TestOrganisation] = {
    generator.generateTestOrganisation(serviceNames, eoriNumber, taxpayerType).flatMap { organisation =>
      val hashedPassword = passwordService.hash(organisation.password)
      testUserRepository.createUser(organisation.copy(password = hashedPassword)) map {
        case createdOrganisation
          if createdOrganisation.services.contains(ServiceKeys.MTD_INCOME_TAX) => desSimulatorConnector.createOrganisation(createdOrganisation)
        case _ => Future.successful(organisation)
      } map {
        _ => organisation
      }
    }
  }

  def createTestAgent(serviceNames: Seq[ServiceKey]) = {
    generator.generateTestAgent(serviceNames).flatMap { agent =>
      val hashedPassword = passwordService.hash(agent.password)
      testUserRepository.createUser(agent.copy(password = hashedPassword)) map (_ => agent)
    }
  }

  def fetchIndividualByNino(nino: Nino): Future[TestIndividual] = {
    testUserRepository.fetchIndividualByNino(nino) map(t => t.getOrElse(throw UserNotFound(INDIVIDUAL)))
  }

  def fetchIndividualByShortNino(shortNino: NinoNoSuffix): Future[TestIndividual] = {
    testUserRepository.fetchIndividualByShortNino(shortNino) map(t => t.getOrElse(throw UserNotFound(INDIVIDUAL)))
  }

  def fetchIndividualBySaUtr(saUtr: SaUtr): Future[TestIndividual] = {
    testUserRepository.fetchIndividualBySaUtr(saUtr) map(t => t.getOrElse(throw UserNotFound(INDIVIDUAL)))
  }

  def fetchIndividualByVrn(vrn: Vrn): Future[TestIndividual] = {
    testUserRepository.fetchIndividualByVrn(vrn) map(t => t.getOrElse(throw UserNotFound(INDIVIDUAL)))
  }

  def fetchOrganisationByEmpRef(empRef: EmpRef): Future[TestOrganisation] = {
    testUserRepository.fetchOrganisationByEmpRef(empRef) map(t => t.getOrElse(throw UserNotFound(ORGANISATION)))
  }

  def fetchOrganisationByVrn(vrn: Vrn): Future[TestOrganisation] = {
    testUserRepository.fetchOrganisationByVrn(vrn) map(t => t.getOrElse(throw UserNotFound(ORGANISATION)))
  }

  def fetchOrganisationByCtUtr(utr: CtUtr): Future[TestOrganisation] = {
    testUserRepository.fetchOrganisationByCtUtr(utr) map(t => t.getOrElse(throw UserNotFound(ORGANISATION)))
  }

  def fetchOrganisationBySaUtr(saUtr: SaUtr): Future[TestOrganisation] = {
    testUserRepository.fetchOrganisationBySaUtr(saUtr) map(t => t.getOrElse(throw UserNotFound(ORGANISATION)))
  }

  def fetchOrganisationByCrn(crn: Crn): Future[TestOrganisation] = {
    testUserRepository.fetchOrganisationByCrn(crn) map(t => t.getOrElse(throw UserNotFound(ORGANISATION)))
  }
}
