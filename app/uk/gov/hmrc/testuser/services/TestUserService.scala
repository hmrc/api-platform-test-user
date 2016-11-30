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

package uk.gov.hmrc.testuser.services

import javax.inject.Inject

import play.api.Logger
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TestUserService {

  val generator: Generator
  val testUserRepository: TestUserRepository
  val passwordService: PasswordService

  def createTestIndividual() = {
    val individual = generator.generateTestIndividual()
    val hashedPassword = passwordService.hash(individual.password)
    testUserRepository.createUser(individual.copy(password = hashedPassword)) map (_ => individual)
  }

  def createTestOrganisation() = {
    val organisation = generator.generateTestOrganisation()
    val hashedPassword = passwordService.hash(organisation.password)
    testUserRepository.createUser(organisation.copy(password = hashedPassword)) map (_ => organisation)
  }

  def authenticate(authReq: AuthenticationRequest): Future[Option[TestUser]] =
    testUserRepository.fetchByUsername(authReq.username).map {
      case None =>
        Logger.debug(s"Username not found: ${authReq.username}")
        None
      case Some(u: TestUser) if passwordService.validate(authReq.password, u.password) =>
        Some(u)
      case _ =>
        Logger.debug(s"Invalid password for username: ${authReq.username}")
        None
    }
}

class TestUserServiceImpl @Inject()(override val passwordService: PasswordServiceImpl) extends TestUserService {
  override val generator: Generator = Generator
  override val testUserRepository = TestUserRepository()
}
