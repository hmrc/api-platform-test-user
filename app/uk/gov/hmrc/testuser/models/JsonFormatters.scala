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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.play.json.Union
import uk.gov.hmrc.testuser.connectors.{Identifier, Enrolment, GovernmentGatewayLogin}

object JsonFormatters {

  implicit val formatObjectId = ReactiveMongoFormats.objectIdFormats
  implicit val formatServiceName = EnumJson.enumFormat(ServiceName)
  implicit val formatUserType = EnumJson.enumFormat(UserType)
  implicit val formatAddress = Json.format[Address]
  implicit val formatIndividualDetails = Json.format[IndividualDetails]
  implicit val formatOrganisationDetails = Json.format[OrganisationDetails]
  implicit val formatTestIndividual = Json.format[TestIndividual]
  implicit val formatTestOrganisation = Json.format[TestOrganisation]
  implicit val formatTestAgent = Json.format[TestAgent]

  implicit val formatTestUser: Format[TestUser] = Union.from[TestUser]("userType")
    .and[TestIndividual](UserType.INDIVIDUAL.toString)
    .and[TestOrganisation](UserType.ORGANISATION.toString)
    .and[TestAgent](UserType.AGENT.toString)
    .format

  implicit val formatCreateTestIndividualResponse = Json.format[TestIndividualCreatedResponse]
  implicit val formatCreateTestOrganisationResponse = Json.format[TestOrganisationCreatedResponse]
  implicit val formatCreateTestAgentResponse = Json.format[TestAgentCreatedResponse]

  implicit val formatAuthenticationRequest = Json.format[AuthenticationRequest]
  implicit val formatAuthenticationResponse = Json.format[AuthenticationResponse]

  implicit val formatCreateUserServicesRequest = Json.format[CreateUserRequest]

  implicit val formatFetchTestIndividualResponse = Json.format[FetchTestIndividualResponse]
  implicit val formatFetchTestOrganisationResponse = Json.format[FetchTestOrganisationResponse]

  implicit val formatErrorCode = EnumJson.enumFormat(ErrorCode)
  implicit val formatErrorResponse = Json.format[ErrorResponse]

  implicit val formatTaxIdentifier = Json.format[Identifier]
  implicit val formatEnrolment = Json.format[Enrolment]
  implicit val formatGovernmentGatewayLogin = Json.format[GovernmentGatewayLogin]

  implicit val formatDesSimulatorTestIndividual = Json.format[DesSimulatorTestIndividual]
  implicit val formatDesSimulatorTestOrganisation = Json.format[DesSimulatorTestOrganisation]

}
