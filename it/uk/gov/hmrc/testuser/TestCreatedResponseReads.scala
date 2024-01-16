/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.testuser

import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.JsonFormatters._
import java.time.LocalDate

object TestCreatedResponseReads {

  implicit val readIndividual: Reads[TestIndividualCreatedResponse] = (
    (JsPath \ "userId").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "userFullName").read[String] and
      (JsPath \ "emailAddress").read[String] and
      (JsPath \ "individualDetails").read[IndividualDetails] and
      (JsPath \ "vatRegistrationDate").readNullable[LocalDate] and
      (JsPath).read[Map[String, JsValue]].map(_.collect { case (key, JsString(value)) => (key -> value) })
  )(TestIndividualCreatedResponse.apply _)

  implicit val readOrganisation: Reads[TestOrganisationCreatedResponse] = (
    (JsPath \ "userId").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "userFullName").read[String] and
      (JsPath \ "emailAddress").read[String] and
      (JsPath \ "organisationDetails").read[OrganisationDetails] and
      (JsPath \ "individualDetails").readNullable[IndividualDetails] and
      (JsPath \ "vatRegistrationDate").readNullable[LocalDate] and
      (JsPath).read[Map[String, JsValue]].map(_.collect { case (key, JsString(value)) => (key -> value) })
  )(TestOrganisationCreatedResponse.apply _)

  implicit val readAgent: Reads[TestAgentCreatedResponse] = (
    (JsPath \ "userId").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "userFullName").read[String] and
      (JsPath \ "emailAddress").read[String] and
      (JsPath).read[Map[String, JsValue]].map(_.collect { case (key, JsString(value)) => (key -> value) })
  )(TestAgentCreatedResponse.apply _)
}
