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

import play.api.libs.json.{OFormat, Reads, Writes, _}
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
  case object groupIdentifier                         extends TestUserPropKey
  case object crn                                     extends TestUserPropKey
  case object taxpayerType                            extends TestUserPropKey

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
    groupIdentifier,
    crn,
    taxpayerType
  )

  def apply(text: String): Option[TestUserPropKey] = TestUserPropKey.values.find(_.toString == text)

  def unsafeApply(text: String): TestUserPropKey     = {
    apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid TestUserPropKey"))
  }
  implicit val write: Writes[TestUserPropKey]        = Writes[TestUserPropKey](b => JsString(b.toString))
  implicit val keyWrites: KeyWrites[TestUserPropKey] = _.toString
}

sealed trait TestUser {
  val userId: String
  val password: String
  val userFullName: String
  val emailAddress: String
  val affinityGroup: String
  val services: Seq[ServiceKey]
}

case class TestOrganisation(
    override val userId: String,
    override val password: String,
    override val userFullName: String,
    override val emailAddress: String,
    organisationDetails: OrganisationDetails,
    individualDetails: Option[IndividualDetails],
    override val services: Seq[ServiceKey] = Seq.empty,
    vatRegistrationDate: Option[LocalDate] = None,
    props: Map[TestUserPropKey, String] = Map.empty
  ) extends TestUser {
  override val affinityGroup = "Organisation"

  lazy val saUtr                                   = props.get(TestUserPropKey.saUtr)
  lazy val nino                                    = props.get(TestUserPropKey.nino)
  lazy val mtdItId                                 = props.get(TestUserPropKey.mtdItId)
  lazy val empRef                                  = props.get(TestUserPropKey.empRef)
  lazy val ctUtr                                   = props.get(TestUserPropKey.ctUtr)
  lazy val vrn                                     = props.get(TestUserPropKey.vrn)
  lazy val lisaManRefNum                           = props.get(TestUserPropKey.lisaManRefNum)
  lazy val secureElectronicTransferReferenceNumber = props.get(TestUserPropKey.secureElectronicTransferReferenceNumber)
  lazy val pensionSchemeAdministratorIdentifier    = props.get(TestUserPropKey.pensionSchemeAdministratorIdentifier)
  lazy val eoriNumber                              = props.get(TestUserPropKey.eoriNumber)
  lazy val groupIdentifier                         = props.get(TestUserPropKey.groupIdentifier)
  lazy val crn                                     = props.get(TestUserPropKey.crn)
  lazy val taxpayerType                            = props.get(TestUserPropKey.taxpayerType)
}

object TestOrganisation {
  import play.api.libs.functional.syntax._

  def convertMap(in: Map[String, JsValue]): Map[TestUserPropKey, String] = {
    in.collect { case (key, JsString(value)) if (TestUserPropKey(key).isDefined) => (TestUserPropKey.unsafeApply(key), value) }
  }

  val reads: Reads[TestOrganisation] = (
    (JsPath \ "userId").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "userFullName").read[String] and
      (JsPath \ "emailAddress").read[String] and
      (JsPath \ "organisationDetails").read[OrganisationDetails] and
      (JsPath \ "individualDetails").readNullable[IndividualDetails] and
      (JsPath \ "services").readWithDefault[Seq[ServiceKey]](Seq.empty) and
      (JsPath \ "vatRegistrationDate").readNullable[LocalDate] and
      (JsPath).read[Map[String, JsValue]].map(convertMap(_))
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
    override val userId: String,
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
    _id: ObjectId = ObjectId.get
  ) extends TestUser {
  override val affinityGroup = "Individual"

}

// case class TestOrganisation(
//     override val userId: String,
//     override val password: String,
//     override val userFullName: String,
//     override val emailAddress: String,
//     organisationDetails: OrganisationDetails,
//     individualDetails: Option[IndividualDetails],
//     saUtr: Option[String] = None,
//     nino: Option[String] = None,
//     mtdItId: Option[String] = None,
//     empRef: Option[String] = None,
//     ctUtr: Option[String] = None,
//     vrn: Option[String] = None,
//     vatRegistrationDate: Option[LocalDate] = None,
//     lisaManRefNum: Option[String] = None,
//     secureElectronicTransferReferenceNumber: Option[String] = None,
//     pensionSchemeAdministratorIdentifier: Option[String] = None,
//     eoriNumber: Option[String] = None,
//     groupIdentifier: Option[String] = None,
//     override val services: Seq[ServiceKey] = Seq.empty,
//     _id: ObjectId = ObjectId.get,
//     crn: Option[String] = None,
//     taxpayerType: Option[String] = None
//   ) extends TestUser {
//   override val affinityGroup = "Organisation"
// }

case class TestAgent(
    override val userId: String,
    override val password: String,
    override val userFullName: String,
    override val emailAddress: String,
    arn: Option[String] = None,
    groupIdentifier: Option[String] = None,
    agentCode: Option[String] = None,
    override val services: Seq[ServiceKey] = Seq.empty,
    _id: ObjectId = ObjectId.get
  ) extends TestUser {
  override val affinityGroup = "Agent"
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
  // Temporarily supports ExciseNumber for EMCS
  require(EoriNumber.isValid(value), s"$value is not a valid EORI/ExciseNumber.")

  override val name = EoriNumber.name
}

object EoriNumber extends SimpleName {
  // Temporarily supports ExciseNumber for EMCS by adding | and the second capturing group
  val validEoriFormat = "^((GB|XI)[0-9]{12,15})|([A-Z]{2}[a-zA-Z0-9]{11})$"

  def isValid(eoriNumber: String) = eoriNumber.matches(validEoriFormat)

  override val name = "eoriNumber"
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
