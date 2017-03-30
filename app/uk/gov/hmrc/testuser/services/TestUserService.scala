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

import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.testuser.connectors.AuthLoginApiConnector
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.LegacySandboxUser._
import uk.gov.hmrc.testuser.repository.TestUserRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

trait TestUserService {

  val generator: Generator
  val testUserRepository: TestUserRepository
  val passwordService: PasswordService
  val authLoginApiConnector: AuthLoginApiConnector

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

  def authenticate(authReq: AuthenticationRequest)(implicit hc: HeaderCarrier): Future[(TestUser, AuthSession)] = {
    val userFuture = authReq match {
      case `sandboxAuthenticationRequest` => successful(sandboxUser)
      case _ => testUserRepository.fetchByUserId(authReq.userId).map {
        case Some(u: TestUser) if passwordService.validate(authReq.password, u.password) => u
        case None =>
          throw InvalidCredentials(s"UserId not found: ${authReq.userId}")
        case _ =>
          throw InvalidCredentials(s"Invalid password for userId: ${authReq.userId}")
      }
    }
    for {
      user <- userFuture
      authSession <- authLoginApiConnector.createSession(user)
    } yield (user, authSession)
  }
}

class TestUserServiceImpl @Inject()(override val passwordService: PasswordServiceImpl,
                                    override val authLoginApiConnector: AuthLoginApiConnector) extends TestUserService {
  override val generator: Generator = Generator
  override val testUserRepository = TestUserRepository()
}
