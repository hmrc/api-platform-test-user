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

package uk.gov.hmrc.testuser.models

import java.time.LocalDate

import play.api.libs.functional.syntax._
import play.api.libs.json._

object PropsRenamer {

  val transform: Map[TestUserPropKey, String] => Map[String, String] = _.map {
    case (TestUserPropKey.arn, v)           => ("agentServicesAccountNumber" -> v)
    case (TestUserPropKey.lisaManRefNum, v) => ("lisaManagerReferenceNumber" -> v)
    case (k, v)                             => k.toString                    -> v
  }
}

sealed trait TestUserResponse {
  def userId: String
  def userFullName: String
  def emailAddress: String
  def props: Map[String, String]
}

sealed trait TestIndividualResponse extends TestUserResponse {
  def individualDetails: IndividualDetails
  def vatRegistrationDate: Option[LocalDate]
}

sealed trait TestOrganisationResponse extends TestUserResponse {
  def organisationDetails: OrganisationDetails
  def individualDetails: Option[IndividualDetails]
  def vatRegistrationDate: Option[LocalDate]
}

case class FetchTestIndividualResponse(
    userId: String,
    userFullName: String,
    emailAddress: String,
    individualDetails: IndividualDetails,
    vatRegistrationDate: Option[LocalDate] = None,
    props: Map[String, String]
  ) extends TestIndividualResponse

object FetchTestIndividualResponse {

  def from(individual: TestIndividual) = FetchTestIndividualResponse(
    individual.userId,
    individual.userFullName,
    individual.emailAddress,
    individual.individualDetails,
    individual.vatRegistrationDate,
    PropsRenamer.transform(individual.props)
  )

  implicit val format: OFormat[FetchTestIndividualResponse] = (
    (JsPath \ "userId").format[String] and
      (JsPath \ "userFullName").format[String] and
      (JsPath \ "emailAddress").format[String] and
      (JsPath \ "individualDetails").format[IndividualDetails] and
      (JsPath \ "vatRegistrationDate").formatNullable[LocalDate] and
      (JsPath).format[Map[String, String]]
  )(FetchTestIndividualResponse.apply, unlift(FetchTestIndividualResponse.unapply _))
}

case class FetchTestOrganisationResponse(
    userId: String,
    userFullName: String,
    emailAddress: String,
    organisationDetails: OrganisationDetails,
    individualDetails: Option[IndividualDetails],
    vatRegistrationDate: Option[LocalDate] = None,
    props: Map[String, String]
  ) extends TestOrganisationResponse

object FetchTestOrganisationResponse {

  def from(organisation: TestOrganisation) = FetchTestOrganisationResponse(
    organisation.userId,
    organisation.userFullName,
    organisation.emailAddress,
    organisation.organisationDetails,
    organisation.individualDetails,
    organisation.vatRegistrationDate,
    PropsRenamer.transform(organisation.props)
  )

  implicit val format: OFormat[FetchTestOrganisationResponse] = (
    (JsPath \ "userId").format[String] and
      (JsPath \ "userFullName").format[String] and
      (JsPath \ "emailAddress").format[String] and
      (JsPath \ "organisationDetails").format[OrganisationDetails] and
      (JsPath \ "individualDetails").formatNullable[IndividualDetails] and
      (JsPath \ "vatRegistrationDate").formatNullable[LocalDate] and
      (JsPath).format[Map[String, String]]
  )(FetchTestOrganisationResponse.apply, unlift(FetchTestOrganisationResponse.unapply _))
}

case class TestIndividualCreatedResponse(
    userId: String,
    password: String,
    userFullName: String,
    emailAddress: String,
    individualDetails: IndividualDetails,
    vatRegistrationDate: Option[LocalDate] = None,
    props: Map[String, String]
  ) extends TestIndividualResponse

object TestIndividualCreatedResponse {

  def from(individual: TestIndividual) = TestIndividualCreatedResponse(
    individual.userId,
    individual.password,
    individual.userFullName,
    individual.emailAddress,
    individual.individualDetails,
    individual.vatRegistrationDate,
    PropsRenamer.transform(individual.props)
  )

  implicit val write: OWrites[TestIndividualCreatedResponse] = (
    (JsPath \ "userId").write[String] and
      (JsPath \ "password").write[String] and
      (JsPath \ "userFullName").write[String] and
      (JsPath \ "emailAddress").write[String] and
      (JsPath \ "individualDetails").write[IndividualDetails] and
      (JsPath \ "vatRegistrationDate").writeNullable[LocalDate] and
      (JsPath).write[Map[String, String]]
  )(unlift(TestIndividualCreatedResponse.unapply _))

}

case class TestOrganisationCreatedResponse(
    userId: String,
    password: String,
    userFullName: String,
    emailAddress: String,
    organisationDetails: OrganisationDetails,
    individualDetails: Option[IndividualDetails],
    vatRegistrationDate: Option[LocalDate] = None,
    props: Map[String, String]
  ) extends TestOrganisationResponse

object TestOrganisationCreatedResponse {

  def from(organisation: TestOrganisation) = TestOrganisationCreatedResponse(
    organisation.userId,
    organisation.password,
    organisation.userFullName,
    organisation.emailAddress,
    organisation.organisationDetails,
    organisation.individualDetails,
    organisation.vatRegistrationDate,
    PropsRenamer.transform(organisation.props)
  )

  implicit val write: OWrites[TestOrganisationCreatedResponse] = (
    (JsPath \ "userId").write[String] and
      (JsPath \ "password").write[String] and
      (JsPath \ "userFullName").write[String] and
      (JsPath \ "emailAddress").write[String] and
      (JsPath \ "organisationDetails").write[OrganisationDetails] and
      (JsPath \ "individualDetails").writeNullable[IndividualDetails] and
      (JsPath \ "vatRegistrationDate").writeNullable[LocalDate] and
      (JsPath).write[Map[String, String]]
  )(unlift(TestOrganisationCreatedResponse.unapply _))
}

case class TestAgentCreatedResponse(
    userId: String,
    password: String,
    userFullName: String,
    emailAddress: String,
    props: Map[String, String]
  ) extends TestUserResponse

object TestAgentCreatedResponse {
  import play.api.libs.functional.syntax._

  def from(agent: TestAgent) = TestAgentCreatedResponse(
    agent.userId,
    agent.password,
    agent.userFullName,
    agent.emailAddress,
    PropsRenamer.transform(agent.props)
  )

  implicit val writes: OWrites[TestAgentCreatedResponse] = (
    (JsPath \ "userId").write[String] and
      (JsPath \ "password").write[String] and
      (JsPath \ "userFullName").write[String] and
      (JsPath \ "emailAddress").write[String] and
      (JsPath).write[Map[String, String]]
  )(unlift(TestAgentCreatedResponse.unapply _))
}
