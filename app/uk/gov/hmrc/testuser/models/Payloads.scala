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

package uk.gov.hmrc.testuser.models

import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.testuser.models.ServiceName.ServiceName

case class AuthenticationRequest(userId: String, password: String)

case class AuthenticationResponse(gatewayToken: String, affinityGroup: String)

case class AuthSession(authBearerToken: String, authorityUri: String, gatewayToken: String)

case class CreateUserRequest(serviceNames: Option[Seq[ServiceName]])


object LegacySandboxUser {
  private val userId = "user1"
  private val password = "password1"
  val sandboxAuthenticationRequest = AuthenticationRequest(userId, password)
  val sandboxUser = TestIndividual(userId, password, SaUtr("1700000000"), Nino("AA000017A"), null)
}

case class InvalidCredentials(msg: String) extends Exception

object ErrorCode extends Enumeration {

  type ErrorCode = Value

  val INTERNAL_SERVER_ERROR = Value("INTERNAL_SERVER_ERROR")
  val INVALID_CREDENTIALS = Value("INVALID_CREDENTIALS")
}

case class ErrorResponse(code: ErrorCode.Value, message: String)

object ErrorResponse {
  val internalServerError = ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
  val invalidCredentialsError = ErrorResponse(ErrorCode.INVALID_CREDENTIALS, "Invalid Authentication information provided")
}
