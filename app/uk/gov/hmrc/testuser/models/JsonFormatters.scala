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
import uk.gov.hmrc.testuser.connectors.{TaxIdentifier, Enrolment, GovernmentGatewayLogin}

object JsonFormatters {

  implicit val formatObjectId = ReactiveMongoFormats.objectIdFormats
  implicit val formatUserType = EnumJson.enumFormat(UserType)
  implicit val formatTestIndividual = Json.format[TestIndividual]
  implicit val formatTestOrganisation = Json.format[TestOrganisation]

  implicit val formatTestUser: Format[TestUser] = Union.from[TestUser]("userType")
    .and[TestIndividual](UserType.INDIVIDUAL.toString)
    .and[TestOrganisation](UserType.ORGANISATION.toString)
    .format

  implicit val formatCreateTestIndividualResponse = Json.format[TestIndividualCreatedResponse]
  implicit val formatCreateTestOrganisationResponse = Json.format[TestOrganisationCreatedResponse]

  implicit val formatAuthenticationRequest = Json.format[AuthenticationRequest]
  implicit val formatAuthenticationResponse = Json.format[AuthenticationResponse]

  implicit val formatTestIndividualResponse = Json.format[TestIndividualResponse]
  implicit val formatTestOrganisationResponse = Json.format[TestOrganisationResponse]

  implicit val formatErrorCode = EnumJson.enumFormat(ErrorCode)
  implicit val formatErrorResponse = Json.format[ErrorResponse]

  implicit val formatTaxIdentifier = Json.format[TaxIdentifier]
  implicit val formatEnrolment = Json.format[Enrolment]
  implicit val formatGovernmentGatewayLogin = Json.format[GovernmentGatewayLogin]
}
