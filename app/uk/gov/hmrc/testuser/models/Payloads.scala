/*
 * Copyright 2019 HM Revenue & Customs
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

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.testuser.models.ServiceKeys._

case class AuthenticationRequest(username: String, password: String)

case class AuthenticationResponse(gatewayToken: String, affinityGroup: String)

case class AuthSession(authBearerToken: String, authorityUri: String, gatewayToken: String)

case class CreateUserRequest(serviceNames: Option[Seq[ServiceKey]])


object LegacySandboxUser {
  private val userId = "user1"
  private val password = "password1"
  private val userFullName = "John Doe"
  private val emailAddress = "john.doe@example.com"
  val sandboxAuthenticationRequest = AuthenticationRequest(userId, password)
  val individualDetails = IndividualDetails("John", "Doe", LocalDate.parse("1980-01-10"),
    Address("221b Baker St", "Marylebone", "NW1 6XE"))
  val sandboxUser = TestIndividual(userId, password, userFullName, emailAddress, individualDetails,
    saUtr = Some(SaUtr("1700000000")), nino = Some(Nino("AA000017A")),
    services = Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT))
}

case class InvalidCredentials(msg: String) extends Exception
case class UserNotFound(userType: UserType.Value) extends Exception

object ErrorCode extends Enumeration {

  type ErrorCode = Value

  val INTERNAL_SERVER_ERROR = Value("INTERNAL_SERVER_ERROR")
  val INVALID_CREDENTIALS = Value("INVALID_CREDENTIALS")
  val USER_NOT_FOUND = Value("USER_NOT_FOUND")
}

case class ErrorResponse(code: ErrorCode.Value, message: String)

object ErrorResponse {
  val internalServerError = ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
  val invalidCredentialsError = ErrorResponse(ErrorCode.INVALID_CREDENTIALS, "Invalid Authentication information provided")
  val individualNotFoundError = ErrorResponse(ErrorCode.USER_NOT_FOUND, "The individual can not be found")
  val organisationNotFoundError = ErrorResponse(ErrorCode.USER_NOT_FOUND, "The organisation can not be found")
}
