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

package uk.gov.hmrc.testuser.models

import java.time.LocalDate

import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats
import uk.gov.hmrc.play.json.Union

import uk.gov.hmrc.testuser.connectors._

object WrapAssortedReadsAndWrites extends EnvReads with EnvWrites

object JsonFormatters {

  implicit val formatLocalDateReads: Reads[LocalDate]   = WrapAssortedReadsAndWrites.DefaultLocalDateReads
  implicit val formatLocalDateWrites: Writes[LocalDate] = WrapAssortedReadsAndWrites.DefaultLocalDateWrites

  implicit val crnFormatter              = Json.format[Crn]
  implicit val formatObjectId            = MongoFormats.objectIdFormat
  implicit val formatServiceName         = EnumJson.enumFormat(ServiceKeys)
  implicit val formatUserType            = EnumJson.enumFormat(UserType)
  implicit val formatAddress             = Json.format[Address]
  implicit val formatIndividualDetails   = Json.format[IndividualDetails]
  implicit val formatOrganisationDetails = Json.format[OrganisationDetails]
  implicit val formatTestIndividual      = Json.format[TestIndividual]
  implicit val formatTestOrganisation    = Json.format[TestOrganisation]
  implicit val formatTestAgent           = Json.format[TestAgent]

  implicit val formatTestUser: Format[TestUser] = Union.from[TestUser]("userType")
    .and[TestIndividual](UserType.INDIVIDUAL.toString)
    .and[TestOrganisation](UserType.ORGANISATION.toString)
    .and[TestAgent](UserType.AGENT.toString)
    .format

  implicit val formatCreateTestIndividualResponse   = Json.format[TestIndividualCreatedResponse]
  implicit val formatCreateTestOrganisationResponse = Json.format[TestOrganisationCreatedResponse]
  implicit val formatCreateTestAgentResponse        = Json.format[TestAgentCreatedResponse]

  implicit val formatAuthenticationRequest  = Json.format[AuthenticationRequest]
  implicit val formatAuthenticationResponse = Json.format[AuthenticationResponse]

  implicit val formatCreateUserServicesRequest = Json.format[CreateUserRequest]

  implicit val formatTaxpayerType                      = Json.valueFormat[TaxpayerType]
  implicit val formatEoriNumber                        = Json.valueFormat[EoriNumber]
  implicit val formatCreateUserWithOptionalEoriRequest = Json.format[CreateUserWithOptionalRequestParams]

  implicit val formatFetchTestIndividualResponse   = Json.format[FetchTestIndividualResponse]
  implicit val formatFetchTestOrganisationResponse = Json.format[FetchTestOrganisationResponse]

  implicit val formatErrorCode     = EnumJson.enumFormat(ErrorCode)
  implicit val formatErrorResponse = Json.format[ErrorResponse]

  implicit val formatTaxIdentifier = Json.format[Identifier]
  implicit val formatEnrolment     = Json.format[Enrolment]

  implicit val formatAuthLoginAddress       = Json.format[AuthLoginAddress]
  implicit val formatItmpData               = Json.format[ItmpData]
  implicit val formatMdtpInformation        = Json.format[MdtpInformation]
  implicit val formatGovernmentGatewayLogin = Json.format[GovernmentGatewayLogin]

  implicit val formatDesSimulatorTestIndividual   = Json.format[DesSimulatorTestIndividual]
  implicit val formatDesSimulatorTestOrganisation = Json.format[DesSimulatorTestOrganisation]

  implicit val formatServices = Json.format[Service]
}
