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

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.testuser.connectors.AuthLoginApiConnector
import uk.gov.hmrc.testuser.models.{AuthSession, AuthenticationRequest, InvalidCredentials, TestUser}
import uk.gov.hmrc.testuser.models.LegacySandboxUser._
import uk.gov.hmrc.testuser.repository.TestUserRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future._

class AuthenticationService @Inject() (
    val passwordService: PasswordService,
    val authLoginApiConnector: AuthLoginApiConnector,
    val testUserRepository: TestUserRepository
  )(implicit ec: ExecutionContext
  ) extends ApplicationLogger {

  def authenticate(authReq: AuthenticationRequest)(implicit hc: HeaderCarrier): Future[(TestUser, AuthSession)] = {
    val userFuture = authReq match {
      case `sandboxAuthenticationRequest` => successful(sandboxUser)
      case _                              => testUserRepository.fetchByUserId(authReq.username).map {
          case Some(u: TestUser) if passwordService.validate(authReq.password, u.password) => u
          case None                                                                        =>
            throw InvalidCredentials(s"User ID not found: ${authReq.username}")
          case _                                                                           =>
            throw InvalidCredentials(s"Invalid password for user ID: ${authReq.username}")
        }
    }
    for {
      testUser    <- userFuture
      authSession <- authLoginApiConnector.createSession(testUser)
    } yield (testUser, authSession)
  }

  def authenticateByCredId(credId: String)(implicit hc: HeaderCarrier): Future[(TestUser, AuthSession)] = {
    val userFuture = testUserRepository.fetchByUserId(credId).map {
      case Some(u: TestUser) => u
      case None              =>
        throw InvalidCredentials(s"User ID not found: ${credId}")
      case _                 =>
        throw InvalidCredentials(s"Invalid credId: ${credId}")
    }
    for {
      user        <- userFuture
      _            = logger.info("Create session by CredId")
      authSession <- authLoginApiConnector.createSession(user)
    } yield (user, authSession)
  }
}
