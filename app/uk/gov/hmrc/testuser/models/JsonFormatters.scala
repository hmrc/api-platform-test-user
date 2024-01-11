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

import org.bson.types.ObjectId

import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats
import uk.gov.hmrc.play.json.Union

import uk.gov.hmrc.testuser.connectors._

object WrapAssortedReadsAndWrites extends EnvReads with EnvWrites

object JsonFormatters {

  implicit val formatLocalDateReads: Reads[LocalDate]   = WrapAssortedReadsAndWrites.DefaultLocalDateReads
  implicit val formatLocalDateWrites: Writes[LocalDate] = WrapAssortedReadsAndWrites.DefaultLocalDateWrites

  implicit val crnFormatter: OFormat[Crn]                              = Json.format[Crn]
  implicit val formatObjectId: Format[ObjectId]                        = MongoFormats.objectIdFormat
  implicit val formatAddress: OFormat[Address]                         = Json.format[Address]
  implicit val formatIndividualDetails: OFormat[IndividualDetails]     = Json.format[IndividualDetails]
  implicit val formatOrganisationDetails: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
  implicit val formatTestIndividual: OFormat[TestIndividual]           = Json.format[TestIndividual]
  implicit val formatTestOrganisation: OFormat[TestOrganisation]       = Json.format[TestOrganisation]
  implicit val formatTestAgent: OFormat[TestAgent]                     = Json.format[TestAgent]

  implicit val formatTestUser: Format[TestUser] = Union.from[TestUser]("userType")
    .and[TestIndividual](UserType.INDIVIDUAL.toString)
    .and[TestOrganisation](UserType.ORGANISATION.toString)
    .and[TestAgent](UserType.AGENT.toString)
    .format

  implicit val formatCreateTestIndividualResponse: OFormat[TestIndividualCreatedResponse]     = Json.format[TestIndividualCreatedResponse]
  implicit val formatCreateTestOrganisationResponse: OFormat[TestOrganisationCreatedResponse] = Json.format[TestOrganisationCreatedResponse]
  implicit val formatCreateTestAgentResponse: OFormat[TestAgentCreatedResponse]               = Json.format[TestAgentCreatedResponse]

  implicit val formatAuthenticationRequest: OFormat[AuthenticationRequest]   = Json.format[AuthenticationRequest]
  implicit val formatAuthenticationResponse: OFormat[AuthenticationResponse] = Json.format[AuthenticationResponse]

  implicit val formatCreateUserServicesRequest: OFormat[CreateUserRequest] = Json.format[CreateUserRequest]

  implicit val formatTaxpayerType: Format[TaxpayerType]                                              = Json.valueFormat[TaxpayerType]
  implicit val formatEoriNumber: Format[EoriNumber]                                                  = Json.valueFormat[EoriNumber]
  implicit val formatCreateUserWithOptionalEoriRequest: OFormat[CreateUserWithOptionalRequestParams] = Json.format[CreateUserWithOptionalRequestParams]

  implicit val formatFetchTestIndividualResponse: OFormat[FetchTestIndividualResponse]     = Json.format[FetchTestIndividualResponse]
  implicit val formatFetchTestOrganisationResponse: OFormat[FetchTestOrganisationResponse] = Json.format[FetchTestOrganisationResponse]

  implicit val formatErrorResponse: OFormat[ErrorResponse] = Json.format[ErrorResponse]

  implicit val formatTaxIdentifier: OFormat[Identifier] = Json.format[Identifier]
  implicit val formatEnrolment: OFormat[Enrolment]      = Json.format[Enrolment]

  implicit val formatAuthLoginAddress: OFormat[AuthLoginAddress]             = Json.format[AuthLoginAddress]
  implicit val formatItmpData: OFormat[ItmpData]                             = Json.format[ItmpData]
  implicit val formatMdtpInformation: OFormat[MdtpInformation]               = Json.format[MdtpInformation]
  implicit val formatGovernmentGatewayLogin: OFormat[GovernmentGatewayLogin] = Json.format[GovernmentGatewayLogin]

  implicit val formatDesSimulatorTestIndividual: OFormat[DesSimulatorTestIndividual]     = Json.format[DesSimulatorTestIndividual]
  implicit val formatDesSimulatorTestOrganisation: OFormat[DesSimulatorTestOrganisation] = Json.format[DesSimulatorTestOrganisation]

}
