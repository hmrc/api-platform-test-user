/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.testuser.models.ServiceKeys.{ServiceKey, Value}
import uk.gov.hmrc.testuser.models.UserType.{AGENT, INDIVIDUAL, ORGANISATION, UserType}

object ServiceKeys extends Enumeration {
  type ServiceKey = Value
  val NATIONAL_INSURANCE: ServiceKeys.Value = Value("national-insurance")
  val SELF_ASSESSMENT: ServiceKeys.Value = Value("self-assessment")
  val CORPORATION_TAX: ServiceKeys.Value = Value("corporation-tax")
  val PAYE_FOR_EMPLOYERS: ServiceKeys.Value = Value("paye-for-employers")
  val SUBMIT_VAT_RETURNS: ServiceKeys.Value = Value("submit-vat-returns")
  val MTD_VAT: ServiceKeys.Value = Value("mtd-vat")
  val MTD_INCOME_TAX: ServiceKeys.Value = Value("mtd-income-tax")
  val AGENT_SERVICES: ServiceKeys.Value = Value("agent-services")
  val LISA: ServiceKeys.Value = Value("lisa")
  val SECURE_ELECTRONIC_TRANSFER: ServiceKeys.Value = Value("secure-electronic-transfer")
  val RELIEF_AT_SOURCE: ServiceKeys.Value = Value("relief-at-source")
  val CUSTOMS_SERVICES: ServiceKeys.Value = Value("customs-services")
  val GOODS_VEHICLE_MOVEMENTS: ServiceKeys.Value = Value("goods-vehicle-movements")
  val ICS_SAFETY_AND_SECURITY: ServiceKeys.Value = Value("ics-safety-and-security")
  val SAFETY_AND_SECURITY: ServiceKeys.Value = Value("safety-and-security")
  val CTC: ServiceKeys.Value = Value("common-transit-convention-traders")
}

case class Service(key: ServiceKey, name: String, allowedUserTypes: Seq[UserType])

object Services extends Seq[Service] {
  val services = Seq(
    Service(ServiceKeys.NATIONAL_INSURANCE, "National Insurance", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKeys.SELF_ASSESSMENT, "Self Assessment", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKeys.CORPORATION_TAX, "Corporation Tax", Seq(ORGANISATION)),
    Service(ServiceKeys.PAYE_FOR_EMPLOYERS, "PAYE for Employers", Seq(ORGANISATION)),
    Service(ServiceKeys.SUBMIT_VAT_RETURNS, "Submit VAT Returns", Seq(ORGANISATION)),
    Service(ServiceKeys.MTD_VAT, "MTD VAT", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKeys.MTD_INCOME_TAX, "MTD Income Tax", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKeys.AGENT_SERVICES, "Agent Services", Seq(AGENT)),
    Service(ServiceKeys.LISA, "Lifetime ISA", Seq(ORGANISATION)),
    Service(ServiceKeys.SECURE_ELECTRONIC_TRANSFER, "Secure Electronic Transfer", Seq(ORGANISATION)),
    Service(ServiceKeys.RELIEF_AT_SOURCE, "Relief at Source", Seq(ORGANISATION)),
    Service(ServiceKeys.CUSTOMS_SERVICES, "Customs Services", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKeys.GOODS_VEHICLE_MOVEMENTS, "Goods Vehicle Services", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKeys.ICS_SAFETY_AND_SECURITY, "ICS Safety and Security", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKeys.CTC, "Common Transit Convention Traders", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKeys.SAFETY_AND_SECURITY, "Safety and Security", Seq(ORGANISATION)))


  override def length: Int = services.length

  override def apply(idx: Int): Service = services.apply(idx)

  override def iterator: Iterator[Service] = services.iterator
}

sealed trait TestUser {
  val userId: String
  val password: String
  val userFullName: String
  val emailAddress: String
  val affinityGroup: String
  val services: Seq[ServiceKey]
  val _id: BSONObjectID
}

case class TestIndividual(override val userId: String,
                          override val password: String,
                          override val userFullName: String,
                          override val emailAddress: String,
                          individualDetails: IndividualDetails,
                          saUtr: Option[String] = None,
                          nino: Option[String] = None,
                          mtdItId: Option[String] = None,
                          vrn: Option[String] = None,
                          vatRegistrationDate: Option[LocalDate] = None,
                          eoriNumber: Option[String] = None,
                          groupIdentifier: Option[String] = None,
                          override val services: Seq[ServiceKey] = Seq.empty,
                          override val _id: BSONObjectID = BSONObjectID.generate) extends TestUser {
  override val affinityGroup = "Individual"
}

case class TestOrganisation(override val userId: String,
                            override val password: String,
                            override val userFullName: String,
                            override val emailAddress: String,
                            organisationDetails: OrganisationDetails,
                            saUtr: Option[String] = None,
                            nino: Option[String] = None,
                            mtdItId: Option[String] = None,
                            empRef: Option[String] = None,
                            ctUtr: Option[String] = None,
                            vrn: Option[String] = None,
                            vatRegistrationDate: Option[LocalDate] = None,
                            lisaManRefNum: Option[String] = None,
                            secureElectronicTransferReferenceNumber: Option[String] = None,
                            pensionSchemeAdministratorIdentifier: Option[String] = None,
                            eoriNumber: Option[String] = None,
                            groupIdentifier: Option[String] = None,
                            override val services: Seq[ServiceKey] = Seq.empty,
                            override val _id: BSONObjectID = BSONObjectID.generate) extends TestUser {
  override val affinityGroup = "Organisation"
}

case class TestAgent(override val userId: String,
                     override val password: String,
                     override val userFullName: String,
                     override val emailAddress: String,
                     arn: Option[String] = None,
                     groupIdentifier: Option[String] = None,
                     override val services: Seq[ServiceKey] = Seq.empty,
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
}

sealed trait TestAgentResponse extends TestUserResponse {
  val agentServicesAccountNumber: Option[String]
  val groupIdentifier: Option[String]
}

case class FetchTestIndividualResponse(override val userId: String,
                                       override val userFullName: String,
                                       override val emailAddress: String,
                                       override val individualDetails: IndividualDetails,
                                       override val saUtr: Option[String] = None,
                                       override val nino: Option[String] = None,
                                       override val mtdItId: Option[String] = None,
                                       override val vrn: Option[String] = None,
                                       override val vatRegistrationDate: Option[LocalDate] = None,
                                       override val eoriNumber: Option[String] = None,
                                       override val groupIdentifier: Option[String] = None)
  extends TestIndividualResponse

object FetchTestIndividualResponse {
  def from(individual: TestIndividual) = FetchTestIndividualResponse(individual.userId, individual.userFullName,
    individual.emailAddress, individual.individualDetails, individual.saUtr, individual.nino,
    individual.mtdItId, individual.vrn, individual.vatRegistrationDate, individual.eoriNumber, individual.groupIdentifier)
}

case class TestIndividualCreatedResponse(override val userId: String,
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
                                         override val groupIdentifier: Option[String] = None)
  extends TestIndividualResponse

object TestIndividualCreatedResponse {
  def from(individual: TestIndividual) = TestIndividualCreatedResponse(individual.userId, individual.password,
    individual.userFullName, individual.emailAddress, individual.individualDetails,
    individual.saUtr, individual.nino, individual.mtdItId, individual.vrn, individual.vatRegistrationDate, individual.eoriNumber,
    individual.groupIdentifier)
}

case class FetchTestOrganisationResponse(override val userId: String,
                                         override val userFullName: String,
                                         override val emailAddress: String,
                                         override val organisationDetails: OrganisationDetails,
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
                                         override val groupIdentifier: Option[String] = None)
  extends TestOrganisationResponse

object FetchTestOrganisationResponse {
  def from(organisation: TestOrganisation) = FetchTestOrganisationResponse(organisation.userId, organisation.userFullName,
    organisation.emailAddress, organisation.organisationDetails, organisation.saUtr, organisation.nino,
    organisation.mtdItId, organisation.empRef, organisation.ctUtr, organisation.vrn, organisation.vatRegistrationDate, organisation.lisaManRefNum,
    organisation.secureElectronicTransferReferenceNumber, organisation.pensionSchemeAdministratorIdentifier,
    organisation.eoriNumber, organisation.groupIdentifier)
}

case class TestOrganisationCreatedResponse(override val userId: String,
                                           password: String,
                                           override val userFullName: String,
                                           override val emailAddress: String,
                                           override val organisationDetails: OrganisationDetails,
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
                                           override val groupIdentifier: Option[String] = None)
  extends TestOrganisationResponse

object TestOrganisationCreatedResponse {
  def from(organisation: TestOrganisation) = TestOrganisationCreatedResponse(organisation.userId, organisation.password,
    organisation.userFullName, organisation.emailAddress, organisation.organisationDetails, organisation.saUtr,
    organisation.nino, organisation.mtdItId, organisation.empRef, organisation.ctUtr, organisation.vrn, organisation.vatRegistrationDate,
    organisation.lisaManRefNum, organisation.secureElectronicTransferReferenceNumber,
    organisation.pensionSchemeAdministratorIdentifier, organisation.eoriNumber, organisation.groupIdentifier)
}

case class TestAgentCreatedResponse(override val userId: String, password: String,
                                    override val userFullName: String,
                                    override val emailAddress: String,
                                    override val agentServicesAccountNumber: Option[String],
                                    override val groupIdentifier: Option[String])
  extends TestAgentResponse

object TestAgentCreatedResponse {
  def from(agent: TestAgent) = TestAgentCreatedResponse(agent.userId, agent.password,
    agent.userFullName, agent.emailAddress, agent.arn, agent.groupIdentifier)
}

case class DesSimulatorTestIndividual(mtdItId: Option[String], vrn: Option[String], nino: Option[String], saUtr: Option[String])

object DesSimulatorTestIndividual {
  def from(individual: TestIndividual) = DesSimulatorTestIndividual(individual.mtdItId, individual.vrn, individual.nino, individual.saUtr)
}

case class DesSimulatorTestOrganisation(mtdItId: Option[String], nino: Option[String],
                                        saUtr: Option[String], ctUtr: Option[String],
                                        empRef: Option[String], vrn: Option[String])

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

