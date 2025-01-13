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

import java.time.{Instant, LocalDate}

import play.api.libs.json._
import uk.gov.hmrc.domain._

case class Address(line1: String, line2: String, postcode: String)

object Address {
  implicit val fmt: Format[Address] = Json.format[Address]
}

case class IndividualDetails(firstName: String, lastName: String, dateOfBirth: LocalDate, address: Address)

object IndividualDetails {
  implicit val fmt: Format[IndividualDetails] = Json.format[IndividualDetails]
}

case class OrganisationDetails(name: String, address: Address)

object OrganisationDetails {
  implicit val fmt: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
}

sealed trait TestUserPropKey

object TestUserPropKey {
  case object saUtr                                   extends TestUserPropKey
  case object nino                                    extends TestUserPropKey
  case object mtdItId                                 extends TestUserPropKey
  case object empRef                                  extends TestUserPropKey
  case object ctUtr                                   extends TestUserPropKey
  case object vrn                                     extends TestUserPropKey
  case object lisaManRefNum                           extends TestUserPropKey
  case object secureElectronicTransferReferenceNumber extends TestUserPropKey
  case object pensionSchemeAdministratorIdentifier    extends TestUserPropKey
  case object eoriNumber                              extends TestUserPropKey
  case object exciseNumber                            extends TestUserPropKey
  case object groupIdentifier                         extends TestUserPropKey
  case object crn                                     extends TestUserPropKey
  case object taxpayerType                            extends TestUserPropKey
  case object arn                                     extends TestUserPropKey
  case object agentCode                               extends TestUserPropKey

  val values: Set[TestUserPropKey] = Set(
    saUtr,
    nino,
    mtdItId,
    empRef,
    ctUtr,
    vrn,
    lisaManRefNum,
    secureElectronicTransferReferenceNumber,
    pensionSchemeAdministratorIdentifier,
    eoriNumber,
    exciseNumber,
    groupIdentifier,
    crn,
    taxpayerType,
    arn,
    agentCode
  )

  def apply(text: String): Option[TestUserPropKey] = TestUserPropKey.values.find(_.toString == text)

  def unsafeApply(text: String): TestUserPropKey     = {
    apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid TestUserPropKey"))
  }
  implicit val write: Writes[TestUserPropKey]        = Writes[TestUserPropKey](b => JsString(b.toString))
  implicit val keyWrites: KeyWrites[TestUserPropKey] = _.toString

  def convertMap(in: Map[String, JsValue]): Map[TestUserPropKey, String] = {
    in.collect { case (key, JsString(value)) if (TestUserPropKey(key).isDefined) => (TestUserPropKey.unsafeApply(key), value) }
  }
}

sealed trait TestUser {
  def userId: String
  def password: String
  def userFullName: String
  def emailAddress: String
  def affinityGroup: String
  def services: Seq[ServiceKey]
}

trait HasTTL {
  self: TestUser =>

  def lastAccess: Instant
}

case class TestOrganisation(
    userId: String,
    password: String,
    userFullName: String,
    emailAddress: String,
    organisationDetails: OrganisationDetails,
    individualDetails: Option[IndividualDetails],
    override val services: Seq[ServiceKey] = Seq.empty,
    vatRegistrationDate: Option[LocalDate] = None,
    props: Map[TestUserPropKey, String] = Map.empty
  ) extends TestUser {
  val affinityGroup = "Organisation"

  // I O
  lazy val saUtr                                   = props.get(TestUserPropKey.saUtr)
  // I O
  lazy val nino                                    = props.get(TestUserPropKey.nino)
  // I O
  lazy val mtdItId                                 = props.get(TestUserPropKey.mtdItId)
  // O
  lazy val empRef                                  = props.get(TestUserPropKey.empRef)
  // O
  lazy val ctUtr                                   = props.get(TestUserPropKey.ctUtr)
  // I O
  lazy val vrn                                     = props.get(TestUserPropKey.vrn)
  // O
  lazy val lisaManRefNum                           = props.get(TestUserPropKey.lisaManRefNum)
  // O
  lazy val secureElectronicTransferReferenceNumber = props.get(TestUserPropKey.secureElectronicTransferReferenceNumber)
  // O
  lazy val pensionSchemeAdministratorIdentifier    = props.get(TestUserPropKey.pensionSchemeAdministratorIdentifier)
  // I O
  lazy val eoriNumber                              = props.get(TestUserPropKey.eoriNumber)
  // O
  lazy val exciseNumber                            = props.get(TestUserPropKey.exciseNumber)
  // I O A
  lazy val groupIdentifier                         = props.get(TestUserPropKey.groupIdentifier)
  // O
  lazy val crn                                     = props.get(TestUserPropKey.crn)
  // O
  lazy val taxpayerType                            = props.get(TestUserPropKey.taxpayerType)
}

object TestOrganisation {
  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads

  val reads: Reads[TestOrganisation] = (
    (JsPath \ "userId").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "userFullName").read[String] and
      (JsPath \ "emailAddress").read[String] and
      (JsPath \ "organisationDetails").read[OrganisationDetails] and
      (JsPath \ "individualDetails").readNullable[IndividualDetails] and
      (JsPath \ "services").readWithDefault[Seq[ServiceKey]](Seq.empty) and
      (JsPath \ "vatRegistrationDate").readNullable[LocalDate] and
      (JsPath).read[Map[String, JsValue]].map(TestUserPropKey.convertMap(_))
  )(TestOrganisation.apply _)

  val writes: OWrites[TestOrganisation] = (
    (JsPath \ "userId").write[String] and
      (JsPath \ "password").write[String] and
      (JsPath \ "userFullName").write[String] and
      (JsPath \ "emailAddress").write[String] and
      (JsPath \ "organisationDetails").write[OrganisationDetails] and
      (JsPath \ "individualDetails").writeNullable[IndividualDetails] and
      (JsPath \ "services").write[Seq[ServiceKey]] and
      (JsPath \ "vatRegistrationDate").writeNullable[LocalDate] and
      (JsPath).write[Map[TestUserPropKey, String]]
  )(unlift(TestOrganisation.unapply _))

  implicit val format: OFormat[TestOrganisation] = OFormat(reads, writes)
}

case class TestIndividual(
    userId: String,
    password: String,
    userFullName: String,
    emailAddress: String,
    individualDetails: IndividualDetails,
    services: Seq[ServiceKey] = Seq.empty,
    vatRegistrationDate: Option[LocalDate] = None,
    props: Map[TestUserPropKey, String] = Map.empty
  ) extends TestUser {
  val affinityGroup                        = "Individual"
  lazy val saUtr: Option[String]           = props.get(TestUserPropKey.saUtr)
  lazy val nino: Option[String]            = props.get(TestUserPropKey.nino)
  lazy val mtdItId: Option[String]         = props.get(TestUserPropKey.mtdItId)
  lazy val vrn: Option[String]             = props.get(TestUserPropKey.vrn)
  lazy val eoriNumber: Option[String]      = props.get(TestUserPropKey.eoriNumber)
  lazy val groupIdentifier: Option[String] = props.get(TestUserPropKey.groupIdentifier)
}

object TestIndividual {
  import play.api.libs.functional.syntax._

  val reads: Reads[TestIndividual] = (
    (JsPath \ "userId").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "userFullName").read[String] and
      (JsPath \ "emailAddress").read[String] and
      (JsPath \ "individualDetails").read[IndividualDetails] and
      (JsPath \ "services").readWithDefault[Seq[ServiceKey]](Seq.empty) and
      (JsPath \ "vatRegistrationDate").readNullable[LocalDate] and
      (JsPath).read[Map[String, JsValue]].map(TestUserPropKey.convertMap(_))
  )(TestIndividual.apply _)

  val writes: OWrites[TestIndividual] = (
    (JsPath \ "userId").write[String] and
      (JsPath \ "password").write[String] and
      (JsPath \ "userFullName").write[String] and
      (JsPath \ "emailAddress").write[String] and
      (JsPath \ "individualDetails").write[IndividualDetails] and
      (JsPath \ "services").write[Seq[ServiceKey]] and
      (JsPath \ "vatRegistrationDate").writeNullable[LocalDate] and
      (JsPath).write[Map[TestUserPropKey, String]]
  )(unlift(TestIndividual.unapply _))

  implicit val format: OFormat[TestIndividual] = OFormat(reads, writes)
}

case class TestAgent(
    userId: String,
    password: String,
    userFullName: String,
    emailAddress: String,
    services: Seq[ServiceKey] = Seq.empty,
    props: Map[TestUserPropKey, String] = Map.empty
  ) extends TestUser {
  val affinityGroup                        = "Agent"
  lazy val arn: Option[String]             = props.get(TestUserPropKey.arn)
  lazy val agentCode: Option[String]       = props.get(TestUserPropKey.agentCode)
  lazy val groupIdentifier: Option[String] = props.get(TestUserPropKey.groupIdentifier)
}

object TestAgent {
  import play.api.libs.functional.syntax._

  val reads: Reads[TestAgent] = (
    (JsPath \ "userId").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "userFullName").read[String] and
      (JsPath \ "emailAddress").read[String] and
      (JsPath \ "services").readWithDefault[Seq[ServiceKey]](Seq.empty) and
      (JsPath).read[Map[String, JsValue]].map(TestUserPropKey.convertMap(_))
  )(TestAgent.apply _)

  val writes: OWrites[TestAgent] = (
    (JsPath \ "userId").write[String] and
      (JsPath \ "password").write[String] and
      (JsPath \ "userFullName").write[String] and
      (JsPath \ "emailAddress").write[String] and
      (JsPath \ "services").write[Seq[ServiceKey]] and
      (JsPath).write[Map[TestUserPropKey, String]]
  )(unlift(TestAgent.unapply _))

  implicit val format: OFormat[TestAgent] = OFormat(reads, writes)

}

case class MtdItId(mtdItId: String) extends TaxIdentifier with SimpleName {
  require(MtdItId.isValid(mtdItId), s"$mtdItId is not a valid MTDITID.")
  override def toString = mtdItId

  def value = mtdItId

  val name = "mtdItId"
}

object MtdItId extends Modulus23Check with (String => MtdItId) {
  implicit val mtdItIdWrite: Writes[MtdItId]  = new SimpleObjectWrites[MtdItId](_.value)
  implicit val mtdItIdRead: Reads[MtdItId]    = new SimpleObjectReads[MtdItId]("mtdItId", MtdItId.apply)
  implicit val mtdItIdFormat: Format[MtdItId] = Format(mtdItIdRead, mtdItIdWrite)

  private val validMtdItIdFormat = "^X[A-Z]IT[0-9]{11}$"

  def isValid(mtdItId: String) = {
    mtdItId.matches(validMtdItIdFormat) && isCheckCorrect(mtdItId, 1)
  }

}

case class LisaManagerReferenceNumber(lisaManagerReferenceNumber: String) extends TaxIdentifier with SimpleName {
  override def toString = lisaManagerReferenceNumber
  val name              = "lisaManagerReferenceNumber"
  def value             = lisaManagerReferenceNumber
}

object LisaManagerReferenceNumber extends (String => LisaManagerReferenceNumber) {
  implicit val lisaManRefNumWrite: Writes[LisaManagerReferenceNumber] = new SimpleObjectWrites[LisaManagerReferenceNumber](_.value)

  implicit val lisaManRefNumRead: Reads[LisaManagerReferenceNumber] =
    new SimpleObjectReads[LisaManagerReferenceNumber]("lisaManagerReferenceNumber", LisaManagerReferenceNumber.apply)
}

case class SecureElectronicTransferReferenceNumber(secureElectronicTransferReferenceNumber: String) extends TaxIdentifier with SimpleName {
  override def toString = secureElectronicTransferReferenceNumber
  val name              = "secureElectronicTransferReferenceNumber"
  def value             = secureElectronicTransferReferenceNumber
}

object SecureElectronicTransferReferenceNumber extends (String => SecureElectronicTransferReferenceNumber) {

  implicit val secureElectronicTransferReferenceNumberWrite: Writes[SecureElectronicTransferReferenceNumber] =
    new SimpleObjectWrites[SecureElectronicTransferReferenceNumber](_.value)

  implicit val secureElectronicTransferReferenceNumberRead: Reads[SecureElectronicTransferReferenceNumber] =
    new SimpleObjectReads[SecureElectronicTransferReferenceNumber]("secureElectronicTransferReferenceNumber", SecureElectronicTransferReferenceNumber.apply)
}

case class PensionSchemeAdministratorIdentifier(pensionSchemeAdministratorIdentifier: String) extends TaxIdentifier with SimpleName {
  override def toString = pensionSchemeAdministratorIdentifier
  val name              = "pensionSchemeAdministratorIdentifier"
  def value             = pensionSchemeAdministratorIdentifier
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
  val validGBorXIEoriFormat = "^(GB|XI)[0-9]{12,15}$"
  val validEUEoriFormat    = "^[A-Z]{2}[0-9]{1,15}$"

  // Because Java doesn't allow conditional regex we need to use this rather clunky mechanism
  // rather than have a single regex.
  def isValid(eoriNumber: String): Boolean = {
    if (eoriNumber.startsWith("GB") || eoriNumber.startsWith("XI")) eoriNumber.matches(validGBorXIEoriFormat)
    else eoriNumber.matches(validEUEoriFormat)
  }

  override val name = "eoriNumber"
}

case class ExciseNumber(override val value: String) extends TaxIdentifier with SimpleName {
  require(ExciseNumber.isValid(value), s"$value is not a valid Excise Number.")

  override val name = ExciseNumber.name
}

object ExciseNumber extends SimpleName {
  val validExciseNumberFormat = "^[A-Z]{2}[a-zA-Z0-9]{11}$"

  def isValid(exciseNumber: String) = exciseNumber.matches(validExciseNumberFormat)

  override val name = "exciseNumber"
}

case class TaxpayerType(override val value: String) extends SimpleName with TaxIdentifier {
  require(TaxpayerType.isValid(value), s"$value is not a valid Taxpayer Type.")

  override val name = "taxpayerType"
}

object TaxpayerType extends SimpleName {
  def isValid(taxpayerType: String) = taxpayerType.trim.toLowerCase == "individual" || taxpayerType.trim.toLowerCase == "partnership"
  override val name                 = "taxpayerType"
}

case class Crn(override val value: String) extends TaxIdentifier with SimpleName {
  require(Crn.isValid(value), s"$value is not a valid CRN.")
  override val name: String = Crn.name
}

object Crn extends SimpleName with (String => Crn) {
  private val validCrnFormat = "^[A-Z0-9]{1,10}$"

  def isValid(value: String) = value.matches(validCrnFormat)

  override val name: String = "crn"

}
