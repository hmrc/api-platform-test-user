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
import play.api.libs.json.{Format, Reads, Writes}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.domain._
import uk.gov.hmrc.testuser.models.ServiceName.ServiceName

object ServiceName extends Enumeration {
  type ServiceName = Value
  val NATIONAL_INSURANCE = Value("national-insurance")
  val SELF_ASSESSMENT = Value("self-assessment")
  val CORPORATION_TAX = Value("corporation-tax")
  val PAYE_FOR_EMPLOYERS = Value("paye-for-employers")
  val SUBMIT_VAT_RETURNS = Value("submit-vat-returns")
  val MTD_VAT = Value("mtd-vat")
  val MTD_INCOME_TAX = Value("mtd-income-tax")
  val AGENT_SERVICES = Value("agent-services")
  val LISA = Value("lisa")
  val SECURE_ELECTRONIC_TRANSFER = Value("secure-electronic-transfer")
  val RELIEF_AT_SOURCE = Value("relief-at-source")
  val CUSTOMS_SERVICES = Value("customs-services")
}

sealed trait TestUser {
  val userId: String
  val password: String
  val userFullName: String
  val emailAddress: String
  val affinityGroup: String
  val services: Seq[ServiceName]
  val _id: BSONObjectID
}

case class TestIndividual(override val userId: String,
                          override val password: String,
                          override val userFullName: String,
                          override val emailAddress: String,
                          individualDetails: IndividualDetails,
                          saUtr: Option[SaUtr] = None,
                          nino: Option[Nino] = None,
                          mtdItId: Option[MtdItId] = None,
                          vrn: Option[Vrn] = None,
                          vatRegistrationDate: Option[LocalDate] = None,
                          eoriNumber: Option[EoriNumber] = None,
                          override val services: Seq[ServiceName] = Seq.empty,
                          override val _id: BSONObjectID = BSONObjectID.generate) extends TestUser {
  override val affinityGroup = "Individual"
}

case class TestOrganisation(override val userId: String,
                            override val password: String,
                            override val userFullName: String,
                            override val emailAddress: String,
                            organisationDetails: OrganisationDetails,
                            saUtr: Option[SaUtr] = None,
                            nino: Option[Nino] = None,
                            mtdItId: Option [MtdItId] = None,
                            empRef: Option[EmpRef] = None,
                            ctUtr: Option[CtUtr] = None,
                            vrn: Option[Vrn] = None,
                            vatRegistrationDate: Option[LocalDate] = None,
                            lisaManRefNum: Option[LisaManagerReferenceNumber] = None,
                            secureElectronicTransferReferenceNumber: Option[SecureElectronicTransferReferenceNumber] = None,
                            pensionSchemeAdministratorIdentifier: Option[PensionSchemeAdministratorIdentifier] = None,
                            eoriNumber: Option[EoriNumber] = None,
                            override val services: Seq[ServiceName] = Seq.empty,
                            override val _id: BSONObjectID = BSONObjectID.generate) extends TestUser {
  override val affinityGroup = "Organisation"
}

case class TestAgent(override val userId: String,
                     override val password: String,
                     override val userFullName: String,
                     override val emailAddress: String,
                     arn: Option[AgentBusinessUtr] = None,
                     override val services: Seq[ServiceName] = Seq.empty,
                     override val _id: BSONObjectID = BSONObjectID.generate) extends TestUser {
  override val affinityGroup = "Agent"
}

sealed trait TestUserResponse {
  val userId: String
  val userFullName: String
  val emailAddress: String
}

sealed trait TestIndividualResponse extends TestUserResponse {
  val individualDetails: IndividualDetails
  val saUtr: Option[SaUtr]
  val nino: Option[Nino]
  val mtdItId: Option[MtdItId]
  val vrn: Option[Vrn]
  val vatRegistrationDate: Option[LocalDate]
  val eoriNumber: Option[EoriNumber]
}

sealed trait TestOrganisationResponse extends TestUserResponse {
  val organisationDetails: OrganisationDetails
  val saUtr: Option[SaUtr]
  val nino: Option[Nino]
  val mtdItId: Option[MtdItId]
  val empRef: Option[EmpRef]
  val ctUtr: Option[CtUtr]
  val vrn: Option[Vrn]
  val vatRegistrationDate: Option[LocalDate]
  val lisaManagerReferenceNumber: Option[LisaManagerReferenceNumber]
  val secureElectronicTransferReferenceNumber: Option[SecureElectronicTransferReferenceNumber]
  val pensionSchemeAdministratorIdentifier: Option[PensionSchemeAdministratorIdentifier]
  val eoriNumber: Option[EoriNumber]
}

sealed trait TestAgentResponse extends TestUserResponse {
  val agentServicesAccountNumber: Option[AgentBusinessUtr]
}

case class FetchTestIndividualResponse(override val userId: String,
                                       override val userFullName: String,
                                       override val emailAddress: String,
                                       override val individualDetails: IndividualDetails,
                                       override val saUtr: Option[SaUtr] = None,
                                       override val nino: Option[Nino] = None,
                                       override val mtdItId: Option[MtdItId] = None,
                                       override val vrn: Option[Vrn] = None,
                                       override val vatRegistrationDate: Option[LocalDate] = None,
                                       override val eoriNumber: Option[EoriNumber] = None)
  extends TestIndividualResponse

object FetchTestIndividualResponse {
  def from(individual: TestIndividual) = FetchTestIndividualResponse(individual.userId, individual.userFullName,
    individual.emailAddress, individual.individualDetails, individual.saUtr, individual.nino,
    individual.mtdItId, individual.vrn, individual.vatRegistrationDate, individual.eoriNumber)
}

case class TestIndividualCreatedResponse(override val userId: String,
                                         password: String,
                                         override val userFullName: String,
                                         override val emailAddress: String,
                                         override val individualDetails: IndividualDetails,
                                         override val saUtr: Option[SaUtr],
                                         override val nino: Option[Nino],
                                         override val mtdItId: Option[MtdItId],
                                         override val vrn: Option[Vrn],
                                         override val vatRegistrationDate: Option[LocalDate] = None,
                                         override val eoriNumber: Option[EoriNumber] = None)
  extends TestIndividualResponse

object TestIndividualCreatedResponse {
  def from(individual: TestIndividual) = TestIndividualCreatedResponse(individual.userId, individual.password,
    individual.userFullName, individual.emailAddress, individual.individualDetails,
    individual.saUtr, individual.nino, individual.mtdItId, individual.vrn, individual.vatRegistrationDate, individual.eoriNumber)
}

case class FetchTestOrganisationResponse(override val userId: String,
                                         override val userFullName: String,
                                         override val emailAddress: String,
                                         override val organisationDetails: OrganisationDetails,
                                         override val saUtr: Option[SaUtr] = None,
                                         override val nino: Option[Nino] = None,
                                         override val mtdItId: Option[MtdItId] = None,
                                         override val empRef: Option[EmpRef] = None,
                                         override val ctUtr: Option[CtUtr] = None,
                                         override val vrn: Option[Vrn] = None,
                                         override val vatRegistrationDate: Option[LocalDate] = None,
                                         override val lisaManagerReferenceNumber: Option[LisaManagerReferenceNumber] = None,
                                         override val secureElectronicTransferReferenceNumber: Option[SecureElectronicTransferReferenceNumber] = None,
                                         override val pensionSchemeAdministratorIdentifier: Option[PensionSchemeAdministratorIdentifier] = None,
                                         override val eoriNumber: Option[EoriNumber] = None)
  extends TestOrganisationResponse

object FetchTestOrganisationResponse {
  def from(organisation: TestOrganisation) = FetchTestOrganisationResponse(organisation.userId, organisation.userFullName,
    organisation.emailAddress, organisation.organisationDetails, organisation.saUtr, organisation.nino,
    organisation.mtdItId, organisation.empRef, organisation.ctUtr, organisation.vrn, organisation.vatRegistrationDate, organisation.lisaManRefNum,
    organisation.secureElectronicTransferReferenceNumber, organisation.pensionSchemeAdministratorIdentifier,
    organisation.eoriNumber)
}

case class TestOrganisationCreatedResponse(override val userId: String,
                                           password: String,
                                           override val userFullName: String,
                                           override val emailAddress: String,
                                           override val organisationDetails: OrganisationDetails,
                                           override val saUtr: Option[SaUtr],
                                           override val nino: Option[Nino],
                                           override val mtdItId: Option[MtdItId],
                                           override val empRef: Option[EmpRef],
                                           override val ctUtr: Option[CtUtr],
                                           override val vrn: Option[Vrn],
                                           override val vatRegistrationDate: Option[LocalDate] = None,
                                           override val lisaManagerReferenceNumber: Option[LisaManagerReferenceNumber],
                                           override val secureElectronicTransferReferenceNumber: Option[SecureElectronicTransferReferenceNumber],
                                           override val pensionSchemeAdministratorIdentifier: Option[PensionSchemeAdministratorIdentifier],
                                           override val eoriNumber: Option[EoriNumber] = None)
  extends TestOrganisationResponse

object TestOrganisationCreatedResponse {
  def from(organisation: TestOrganisation) = TestOrganisationCreatedResponse(organisation.userId, organisation.password,
    organisation.userFullName, organisation.emailAddress, organisation.organisationDetails, organisation.saUtr,
    organisation.nino, organisation.mtdItId, organisation.empRef, organisation.ctUtr, organisation.vrn, organisation.vatRegistrationDate,
    organisation.lisaManRefNum, organisation.secureElectronicTransferReferenceNumber,
    organisation.pensionSchemeAdministratorIdentifier, organisation.eoriNumber)
}

case class TestAgentCreatedResponse(override val userId: String, password: String,
                                    override val userFullName: String,
                                    override val emailAddress: String,
                                    override val agentServicesAccountNumber: Option[AgentBusinessUtr])
  extends TestAgentResponse

object TestAgentCreatedResponse {
  def from(agent: TestAgent) = TestAgentCreatedResponse(agent.userId, agent.password,
    agent.userFullName, agent.emailAddress, agent.arn)
}

case class DesSimulatorTestIndividual(mtdItId: Option[MtdItId], vrn: Option[Vrn], nino: Option[Nino], saUtr: Option[SaUtr])

object DesSimulatorTestIndividual {
  def from(individual: TestIndividual) = DesSimulatorTestIndividual(individual.mtdItId, individual.vrn, individual.nino, individual.saUtr)
}

case class DesSimulatorTestOrganisation(mtdItId: Option[MtdItId], nino: Option[Nino],
                                        saUtr: Option[SaUtr], ctUtr: Option[CtUtr],
                                        empRef: Option[EmpRef], vrn: Option[Vrn])

object DesSimulatorTestOrganisation {
  def from(organisation: TestOrganisation) = DesSimulatorTestOrganisation(organisation.mtdItId,
    organisation.nino, organisation.saUtr, organisation.ctUtr, organisation.empRef, organisation.vrn)
}

case class MtdItId(mtdItId: String) extends TaxIdentifier with SimpleName {
  require(MtdItId.isValid(mtdItId), s"$mtdItId is not a valid MTDITID.")
  override def toString = mtdItId

  def value = mtdItId

  val name = "mtdItId"
}


object MtdItId extends Modulus23Check with (String => MtdItId) {
  implicit val mtdItIdWrite: Writes[MtdItId] = new SimpleObjectWrites[MtdItId](_.value)
  implicit val mtdItIdRead: Reads[MtdItId] = new SimpleObjectReads[MtdItId]("mtdItId", MtdItId.apply)
  implicit val mtdItIdFormat: Format[MtdItId] = Format(mtdItIdRead, mtdItIdWrite)

  private val validMtdItIdFormat = "^X[A-Z]IT[0-9]{11}$"

  def isValid(mtdItId: String) = {
    mtdItId.matches(validMtdItIdFormat) && isCheckCorrect(mtdItId, 1)
  }

}

case class LisaManagerReferenceNumber(lisaManagerReferenceNumber: String) extends TaxIdentifier with SimpleName {
  override def toString = lisaManagerReferenceNumber
  val name = "lisaManagerReferenceNumber"
  def value = lisaManagerReferenceNumber
}

object LisaManagerReferenceNumber extends (String => LisaManagerReferenceNumber) {
  implicit val lisaManRefNumWrite: Writes[LisaManagerReferenceNumber] = new SimpleObjectWrites[LisaManagerReferenceNumber](_.value)
  implicit val lisaManRefNumRead: Reads[LisaManagerReferenceNumber] = new SimpleObjectReads[LisaManagerReferenceNumber]("lisaManagerReferenceNumber", LisaManagerReferenceNumber.apply)
}

case class SecureElectronicTransferReferenceNumber(secureElectronicTransferReferenceNumber: String) extends TaxIdentifier with SimpleName {
  override def toString = secureElectronicTransferReferenceNumber
  val name = "secureElectronicTransferReferenceNumber"
  def value = secureElectronicTransferReferenceNumber
}

object SecureElectronicTransferReferenceNumber extends (String => SecureElectronicTransferReferenceNumber) {
  implicit val secureElectronicTransferReferenceNumberWrite: Writes[SecureElectronicTransferReferenceNumber] =
    new SimpleObjectWrites[SecureElectronicTransferReferenceNumber](_.value)

  implicit val secureElectronicTransferReferenceNumberRead: Reads[SecureElectronicTransferReferenceNumber] =
    new SimpleObjectReads[SecureElectronicTransferReferenceNumber]("secureElectronicTransferReferenceNumber", SecureElectronicTransferReferenceNumber.apply)
}

case class PensionSchemeAdministratorIdentifier(pensionSchemeAdministratorIdentifier: String) extends TaxIdentifier with SimpleName {
  override def toString = pensionSchemeAdministratorIdentifier
  val name = "pensionSchemeAdministratorIdentifier"
  def value = pensionSchemeAdministratorIdentifier
}

object PensionSchemeAdministratorIdentifier extends (String => PensionSchemeAdministratorIdentifier) {
  implicit val pensionSchemeAdministratorIdentifierWrite: Writes[PensionSchemeAdministratorIdentifier] =
    new SimpleObjectWrites[PensionSchemeAdministratorIdentifier](_.value)

  implicit val pensionSchemeAdministratorIdentifierRead: Reads[PensionSchemeAdministratorIdentifier] =
    new SimpleObjectReads[PensionSchemeAdministratorIdentifier]("pensionSchemeAdministratorIdentifier", PensionSchemeAdministratorIdentifier.apply)
}

case class EoriNumber(override val value: String) extends TaxIdentifier with SimpleName {
  require(EoriNumber.isValid(value), s"$value is not a valid EORI.")

  override val name = EoriNumber.name
}

object EoriNumber extends SimpleName {
  val validEoriFormat = "^[A-z]{2}[0-9]{10,15}$"

  def isValid(eoriNumber: String) = eoriNumber.matches(validEoriFormat)

  override val name = "eoriNumber"

  implicit val jsonFormat = Format[EoriNumber](
    new SimpleObjectReads[EoriNumber](name, EoriNumber.apply),
    new SimpleObjectWrites[EoriNumber](_.value)
  )
}

case class Address(line1: String, line2: String, postcode: String)

case class IndividualDetails(firstName: String, lastName: String, dateOfBirth: LocalDate, address: Address)

case class OrganisationDetails(name: String, address: Address)
