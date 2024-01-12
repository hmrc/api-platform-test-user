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

sealed trait TestUserResponse {
  val userId: String
  val userFullName: String
  val emailAddress: String
}

sealed trait TestIndividualResponse extends TestUserResponse {
  val individualDetails: IndividualDetails
  val saUtr: Option[String]
  val nino: Option[String]
  val mtdItId: Option[String]
  val vrn: Option[String]
  val vatRegistrationDate: Option[LocalDate]
  val eoriNumber: Option[String]
  val groupIdentifier: Option[String]
}

sealed trait TestOrganisationResponse extends TestUserResponse {
  val organisationDetails: OrganisationDetails
  val individualDetails: Option[IndividualDetails]
  val saUtr: Option[String]
  val nino: Option[String]
  val mtdItId: Option[String]
  val empRef: Option[String]
  val ctUtr: Option[String]
  val vrn: Option[String]
  val vatRegistrationDate: Option[LocalDate]
  val lisaManagerReferenceNumber: Option[String]
  val secureElectronicTransferReferenceNumber: Option[String]
  val pensionSchemeAdministratorIdentifier: Option[String]
  val eoriNumber: Option[String]
  val groupIdentifier: Option[String]
  val crn: Option[String]
  val taxpayerType: Option[String]
}

sealed trait TestAgentResponse extends TestUserResponse {
  val agentServicesAccountNumber: Option[String]
  val groupIdentifier: Option[String]
}

case class FetchTestIndividualResponse(
    override val userId: String,
    override val userFullName: String,
    override val emailAddress: String,
    override val individualDetails: IndividualDetails,
    override val saUtr: Option[String] = None,
    override val nino: Option[String] = None,
    override val mtdItId: Option[String] = None,
    override val vrn: Option[String] = None,
    override val vatRegistrationDate: Option[LocalDate] = None,
    override val eoriNumber: Option[String] = None,
    override val groupIdentifier: Option[String] = None
  ) extends TestIndividualResponse

object FetchTestIndividualResponse {

  def from(individual: TestIndividual) = FetchTestIndividualResponse(
    individual.userId,
    individual.userFullName,
    individual.emailAddress,
    individual.individualDetails,
    individual.saUtr,
    individual.nino,
    individual.mtdItId,
    individual.vrn,
    individual.vatRegistrationDate,
    individual.eoriNumber,
    individual.groupIdentifier
  )
}

case class TestIndividualCreatedResponse(
    override val userId: String,
    password: String,
    override val userFullName: String,
    override val emailAddress: String,
    override val individualDetails: IndividualDetails,
    override val saUtr: Option[String],
    override val nino: Option[String],
    override val mtdItId: Option[String],
    override val vrn: Option[String],
    override val vatRegistrationDate: Option[LocalDate] = None,
    override val eoriNumber: Option[String] = None,
    override val groupIdentifier: Option[String] = None
  ) extends TestIndividualResponse

object TestIndividualCreatedResponse {

  def from(individual: TestIndividual) = TestIndividualCreatedResponse(
    individual.userId,
    individual.password,
    individual.userFullName,
    individual.emailAddress,
    individual.individualDetails,
    individual.saUtr,
    individual.nino,
    individual.mtdItId,
    individual.vrn,
    individual.vatRegistrationDate,
    individual.eoriNumber,
    individual.groupIdentifier
  )
}

case class FetchTestOrganisationResponse(
    override val userId: String,
    override val userFullName: String,
    override val emailAddress: String,
    override val organisationDetails: OrganisationDetails,
    override val individualDetails: Option[IndividualDetails],
    override val saUtr: Option[String] = None,
    override val nino: Option[String] = None,
    override val mtdItId: Option[String] = None,
    override val empRef: Option[String] = None,
    override val ctUtr: Option[String] = None,
    override val vrn: Option[String] = None,
    override val vatRegistrationDate: Option[LocalDate] = None,
    override val lisaManagerReferenceNumber: Option[String] = None,
    override val secureElectronicTransferReferenceNumber: Option[String] = None,
    override val pensionSchemeAdministratorIdentifier: Option[String] = None,
    override val eoriNumber: Option[String] = None,
    override val groupIdentifier: Option[String] = None,
    override val crn: Option[String] = None,
    override val taxpayerType: Option[String] = None
  ) extends TestOrganisationResponse

object FetchTestOrganisationResponse {

  def from(organisation: TestOrganisation) = FetchTestOrganisationResponse(
    organisation.userId,
    organisation.userFullName,
    organisation.emailAddress,
    organisation.organisationDetails,
    organisation.individualDetails,
    organisation.saUtr,
    organisation.nino,
    organisation.mtdItId,
    organisation.empRef,
    organisation.ctUtr,
    organisation.vrn,
    organisation.vatRegistrationDate,
    organisation.lisaManRefNum,
    organisation.secureElectronicTransferReferenceNumber,
    organisation.pensionSchemeAdministratorIdentifier,
    organisation.eoriNumber,
    organisation.groupIdentifier,
    organisation.crn,
    organisation.taxpayerType
  )
}

case class TestOrganisationCreatedResponse(
    override val userId: String,
    password: String,
    override val userFullName: String,
    override val emailAddress: String,
    override val organisationDetails: OrganisationDetails,
    override val individualDetails: Option[IndividualDetails],
    override val saUtr: Option[String],
    override val nino: Option[String],
    override val mtdItId: Option[String],
    override val empRef: Option[String],
    override val ctUtr: Option[String],
    override val vrn: Option[String],
    override val vatRegistrationDate: Option[LocalDate] = None,
    override val lisaManagerReferenceNumber: Option[String],
    override val secureElectronicTransferReferenceNumber: Option[String],
    override val pensionSchemeAdministratorIdentifier: Option[String],
    override val eoriNumber: Option[String] = None,
    override val groupIdentifier: Option[String] = None,
    override val crn: Option[String] = None,
    override val taxpayerType: Option[String] = None
  ) extends TestOrganisationResponse

object TestOrganisationCreatedResponse {

  def from(organisation: TestOrganisation) = TestOrganisationCreatedResponse(
    organisation.userId,
    organisation.password,
    organisation.userFullName,
    organisation.emailAddress,
    organisation.organisationDetails,
    organisation.individualDetails,
    organisation.saUtr,
    organisation.nino,
    organisation.mtdItId,
    organisation.empRef,
    organisation.ctUtr,
    organisation.vrn,
    organisation.vatRegistrationDate,
    organisation.lisaManRefNum,
    organisation.secureElectronicTransferReferenceNumber,
    organisation.pensionSchemeAdministratorIdentifier,
    organisation.eoriNumber,
    organisation.groupIdentifier,
    organisation.crn,
    organisation.taxpayerType
  )
}

case class TestAgentCreatedResponse(
    override val userId: String,
    password: String,
    override val userFullName: String,
    override val emailAddress: String,
    override val agentServicesAccountNumber: Option[String],
    val agentCode: Option[String],
    override val groupIdentifier: Option[String]
  ) extends TestAgentResponse

object TestAgentCreatedResponse {

  def from(agent: TestAgent) = TestAgentCreatedResponse(
    agent.userId,
    agent.password,
    agent.userFullName,
    agent.emailAddress,
    agent.arn,
    agent.agentCode,
    agent.groupIdentifier
  )
}
