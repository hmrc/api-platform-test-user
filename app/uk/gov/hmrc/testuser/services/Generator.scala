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

package uk.gov.hmrc.testuser.services

import org.joda.time.LocalDate
import org.scalacheck.Gen
import uk.gov.hmrc.domain._
import uk.gov.hmrc.testuser.models.ServiceName._
import uk.gov.hmrc.testuser.models.{ServiceName => _, _}
import uk.gov.hmrc.testuser.util.Randomiser

import scala.annotation.tailrec
import scala.util.Random


trait Generator extends Randomiser {

  private val userIdGenerator = Gen.listOfN(12, Gen.numChar).map(_.mkString)
  private val passwordGenerator = Gen.listOfN(12, Gen.alphaNumChar).map(_.mkString)
  private val utrGenerator = new SaUtrGenerator()
  private val ninoGenerator = new uk.gov.hmrc.domain.Generator()
  private val employerReferenceGenerator: Gen[EmpRef] = for {
    taxOfficeNumber <- Gen.choose(100, 999).map(x => x.toString)
    taxOfficeReference <- Gen.listOfN(10, Gen.alphaNumChar).map(_.mkString.toUpperCase)
  } yield EmpRef.fromIdentifiers(s"$taxOfficeNumber/$taxOfficeReference")
  private val vrnGenerator = Gen.choose(666000000, 666999999)
  private val arnGenerator = new ArnGenerator()
  private val mtdItIdGenerator = new MtdItIdGenerator()
  private val lisaManRefNumGenerator = new LisaGenerator()
  private val setRefNumGenerator = new SecureElectronicTransferReferenceNumberGenerator()
  private val psaIdGenerator = new PensionSchemeAdministratorIdentifierGenerator()
  private val eoriGenerator = Gen.listOfN(10, Gen.numChar).map("GB" + _.mkString).map(EoriNumber.apply)

  def generateTestIndividual(services: Seq[ServiceName] = Seq.empty) = {
    val saUtr = if (services.contains(SELF_ASSESSMENT)) Some(generateSaUtr) else None
    val nino = if (services.contains(NATIONAL_INSURANCE) || services.contains(MTD_INCOME_TAX)) Some(generateNino) else None
    val mtdItId = if(services.contains(MTD_INCOME_TAX)) Some(generateMtdId) else None
    val eoriNumber = if(services.contains(CUSTOMS_SERVICES)) Some(generateEoriNumber) else None
    val individualDetails = generateIndividualDetails
    val userFullName = generateUserFullName(individualDetails.firstName, individualDetails.lastName)
    val emailAddress = generateEmailAddress(individualDetails.firstName, individualDetails.lastName)

    TestIndividual(generateUserId, generatePassword, userFullName, emailAddress, individualDetails, saUtr, nino, mtdItId, eoriNumber, services)
  }

  def generateTestOrganisation(services: Seq[ServiceName] = Seq.empty) = {
    val saUtr = if (services.contains(SELF_ASSESSMENT)) Some(generateSaUtr) else None
    val nino = if (services.contains(NATIONAL_INSURANCE) || services.contains(MTD_INCOME_TAX)) Some(generateNino) else None
    val mtdItId = if (services.contains(MTD_INCOME_TAX)) Some(generateMtdId) else None
    val empRef = if (services.contains(PAYE_FOR_EMPLOYERS)) Some(generateEmpRef) else None
    val ctUtr = if (services.contains(CORPORATION_TAX)) Some(generateCtUtr) else None
    val vrn = if (services.contains(SUBMIT_VAT_RETURNS)) Some(generateVrn) else None
    val lisaManRefNum = if (services.contains(LISA)) Some(generateLisaManRefNum) else None
    val setRefNum = if (services.contains(SECURE_ELECTRONIC_TRANSFER)) Some(generateSetRefNum) else None
    val psaId = if(services.contains(RELIEF_AT_SOURCE)) Some(generatePsaId) else None
    val eoriNumber = if(services.contains(CUSTOMS_SERVICES)) Some(generateEoriNumber) else None

    val firstName = generateFirstName
    val lastName = generateLastName
    val userFullName = generateUserFullName(firstName, lastName)
    val emailAddress = generateEmailAddress(firstName, lastName)

    TestOrganisation(generateUserId, generatePassword, userFullName, emailAddress, generateOrganisationDetails, saUtr, nino, mtdItId, empRef, ctUtr,
      vrn, lisaManRefNum, setRefNum, psaId, eoriNumber, services)
  }

  def generateTestAgent(services: Seq[ServiceName] = Seq.empty) = {
    val arn = if (services.contains(AGENT_SERVICES)) Some(generateArn) else None
    val firstName = generateFirstName
    val lastName = generateLastName
    val userFullName = generateUserFullName(firstName, lastName)
    val emailAddress = generateEmailAddress(firstName, lastName)

    TestAgent(generateUserId, generatePassword, userFullName, emailAddress, arn, services)
  }

  def generateUserFullName(firstName: String, lastName: String) = s"$firstName $lastName"

  def generateEmailAddress(firstName: String, lastName: String) = s"$firstName.$lastName@example.com".toLowerCase

  def generateFirstName = randomConfigString("randomiser.individualDetails.firstName")

  def generateLastName = randomConfigString("randomiser.individualDetails.lastName")

  private def generateAddress() = {
    Address(
      randomConfigString("randomiser.address.line1"),
      randomConfigString("randomiser.address.line2"),
      randomConfigString("randomiser.address.postcode")
    )
  }

  private def generateIndividualDetails = {
    IndividualDetails(
      generateFirstName,
      generateLastName,
      LocalDate.parse(randomConfigString("randomiser.individualDetails.dateOfBirth")),
      generateAddress()
    )
  }

  private def generateOrganisationDetails = {
    val randomOrganisationName = "Company " + Gen.listOfN(6, Gen.alphaNumChar).map(_.mkString.toUpperCase).sample.get
    OrganisationDetails(randomOrganisationName, generateAddress())
  }

  private def generateUserId = userIdGenerator.sample.get
  private def generatePassword = passwordGenerator.sample.get
  private def generateEmpRef: EmpRef = employerReferenceGenerator.sample.get
  private def generateSaUtr: SaUtr = utrGenerator.nextSaUtr
  private def generateNino: Nino = ninoGenerator.nextNino
  private def generateCtUtr: CtUtr = CtUtr(utrGenerator.nextSaUtr.value)
  private def generateVrn: Vrn = Vrn(vrnGenerator.sample.get.toString)
  private def generateLisaManRefNum: LisaManagerReferenceNumber = lisaManRefNumGenerator.next
  private def generateSetRefNum: SecureElectronicTransferReferenceNumber = setRefNumGenerator.next
  private def generatePsaId: PensionSchemeAdministratorIdentifier = psaIdGenerator.next
  private def generateArn: AgentBusinessUtr = arnGenerator.next
  private def generateMtdId: MtdItId = mtdItIdGenerator.next
  private def generateEoriNumber: EoriNumber = eoriGenerator.sample.get
}

object Generator extends Generator

class ArnGenerator(random: Random = new Random) extends Modulus23Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: AgentBusinessUtr = {
    val randomCode = "ARN" + f"${random.nextInt(1000000)}%07d"
    val checkCharacter  = calculateCheckCharacter(randomCode)
    AgentBusinessUtr(s"$checkCharacter$randomCode")
  }
}

class MtdItIdGenerator(random: Random = new Random) extends Modulus23Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next = {
    val randomCode = "IT" + f"${random.nextInt(1000000)}%011d"
    val checkCharacter = calculateCheckCharacter(randomCode)
    MtdItId(s"X$checkCharacter$randomCode")
  }
}

class LisaGenerator(random: Random = new Random) extends Modulus23Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: LisaManagerReferenceNumber = {
    val randomCode = f"${random.nextInt(999999)}%06d"
    LisaManagerReferenceNumber(s"Z$randomCode")
  }
}

class SecureElectronicTransferReferenceNumberGenerator(random: Random = new Random) {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: SecureElectronicTransferReferenceNumber = {
    // SecureElectronicTransferReferenceNumber must be 12 digit number not beginning with 0
    val initialDigit = random.nextInt(8) + 1
    val remainingDigits = f"${random.nextInt(Int.MaxValue)}%011d"
    SecureElectronicTransferReferenceNumber(s"$initialDigit$remainingDigits")
  }
}

class PensionSchemeAdministratorIdentifierGenerator(random: Random = new Random) {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: PensionSchemeAdministratorIdentifier = {
    // PensionSchemeAdministratorIdentifier must conform to this regex: ^[Aa]{1}[0-9]{7} e.g. A1234567
    val initialCharacter = if (random.nextBoolean()) "A" else "a"
    val remainingDigits = (for (i <- 1 to 7) yield random.nextInt(9)).mkString("")
    PensionSchemeAdministratorIdentifier(s"$initialCharacter$remainingDigits")
  }
}
