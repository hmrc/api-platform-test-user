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

package uk.gov.hmrc.testuser.services

import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import org.scalacheck.Gen
import play.api.Logger
import uk.gov.hmrc.domain._
import uk.gov.hmrc.testuser.models.ServiceKeys._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.util.Randomiser

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class Generator @Inject()(val testUserRepository: TestUserRepository)(implicit ec: ExecutionContext) extends Randomiser {

  private val userIdGenerator = Gen.listOfN(12, Gen.numChar).map(_.mkString)
  private val passwordGenerator = Gen.listOfN(12, Gen.alphaNumChar).map(_.mkString)
  private val defaultGenerator = new DefaultGenerator()
  private val ninoGenerator = new uk.gov.hmrc.domain.Generator()
  private val employerReferenceGenerator: Gen[EmpRef] = for {
    taxOfficeNumber <- Gen.choose(100, 999).map(x => x.toString)
    taxOfficeReference <- Gen.listOfN(10, Gen.alphaNumChar).map(_.mkString.toUpperCase)
  } yield EmpRef.fromIdentifiers(s"$taxOfficeNumber/$taxOfficeReference")

  private val vrnGenerator: Gen[String] = Gen.choose(1000000, 9999999).map(i => VrnChecksum.apply(i.toString)).retryUntil(VrnChecksum.isValid)
  private val mtdItIdGenerator = new MtdItIdGenerator()
  private val lisaManRefNumGenerator = new LisaGenerator()
  private val setRefNumGenerator = new SecureElectronicTransferReferenceNumberGenerator()
  private val psaIdGenerator = new PensionSchemeAdministratorIdentifierGenerator()
  private val eoriGenerator = Gen.listOfN(10, Gen.numChar).map("GB" + _.mkString).map(EoriNumber.apply)
  private val arnGenerator = new ArnGenerator()

  def generateTestIndividual(services: Seq[ServiceKey] = Seq.empty): Future[TestIndividual] = {
    for {
      saUtr <- if (services.contains(SELF_ASSESSMENT)) generateSaUtr.map(Some(_)) else Future.successful(None)
      nino <- if (services.contains(NATIONAL_INSURANCE) || services.contains(MTD_INCOME_TAX)) generateNino.map(Some(_)) else Future.successful(None)
      mtdItId <- if(services.contains(MTD_INCOME_TAX)) generateMtdId.map(Some(_)) else Future.successful(None)
      eoriNumber <- if(services.contains(CUSTOMS_SERVICES)) generateEoriNumber.map(Some(_)) else Future.successful(None)
      vrn <- if(services.contains(MTD_VAT)) generateVrn.map(Some(_)) else Future.successful(None)
      vatRegistrationDate = vrn.map(_ => LocalDate.now.minusYears(Gen.chooseNum(1,20).sample.get))

      individualDetails = generateIndividualDetails
      userFullName = generateUserFullName(individualDetails.firstName, individualDetails.lastName)
      emailAddress = generateEmailAddress(individualDetails.firstName, individualDetails.lastName)
    } yield
      TestIndividual(
        generateUserId,
        generatePassword,
        userFullName,
        emailAddress,
        individualDetails,
        saUtr,
        nino,
        mtdItId,
        vrn,
        vatRegistrationDate,
        eoriNumber,
        services)
  }

  def generateTestOrganisation(services: Seq[ServiceKey] = Seq.empty): Future[TestOrganisation] = {
    for {
      saUtr <- if (services.contains(SELF_ASSESSMENT)) generateSaUtr.map(Some(_)) else Future.successful(None)
      nino <- if (services.contains(NATIONAL_INSURANCE) || services.contains(MTD_INCOME_TAX)) generateNino.map(Some(_)) else Future.successful(None)
      mtdItId <- if (services.contains(MTD_INCOME_TAX)) generateMtdId.map(Some(_)) else Future.successful(None)
      empRef <- if (services.contains(PAYE_FOR_EMPLOYERS)) generateEmpRef.map(Some(_)) else Future.successful(None)
      ctUtr <- if (services.contains(CORPORATION_TAX)) generateCtUtr.map(Some(_)) else Future.successful(None)
      vrn <- if (services.contains(SUBMIT_VAT_RETURNS) || services.contains(MTD_VAT)) generateVrn.map(Some(_)) else Future.successful(None)
      vatRegistrationDate = vrn.map(_ => LocalDate.now.minusYears(Gen.chooseNum(1, 20).sample.get))
      lisaManRefNum <- if (services.contains(LISA)) generateLisaManRefNum.map(Some(_)) else Future.successful(None)
      setRefNum = if (services.contains(SECURE_ELECTRONIC_TRANSFER)) Some(generateSetRefNum) else None
      psaId = if (services.contains(RELIEF_AT_SOURCE)) Some(generatePsaId) else None
      eoriNumber <- if (services.contains(CUSTOMS_SERVICES)) generateEoriNumber.map(Some(_)) else Future.successful(None)

      firstName = generateFirstName
      lastName = generateLastName
      userFullName = generateUserFullName(firstName, lastName)
      emailAddress = generateEmailAddress(firstName, lastName)
    } yield
      TestOrganisation(
        generateUserId,
        generatePassword,
        userFullName,
        emailAddress,
        generateOrganisationDetails,
        saUtr,
        nino,
        mtdItId,
        empRef,
        ctUtr,
        vrn,
        vatRegistrationDate,
        lisaManRefNum,
        setRefNum,
        psaId,
        eoriNumber,
        services)
  }

  def generateTestAgent(services: Seq[ServiceKey] = Seq.empty) = {
    val firstName = generateFirstName
    val lastName = generateLastName
    val userFullName = generateUserFullName(firstName, lastName)
    val emailAddress = generateEmailAddress(firstName, lastName)

    (if (services.contains(AGENT_SERVICES)) generateArn.map(Some(_)) else Future.successful(None))
      .map(arn => TestAgent(generateUserId, generatePassword, userFullName, emailAddress, arn, services))

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

  private def generateUniqueIdentifier[T <: String](generatorFunction: () => T, count: Int = 1)(implicit ec: ExecutionContext): Future[T] = {
    Logger.info(s"Generating tax identifier attempt $count")
    val generatedIdentifier = generatorFunction()
    testUserRepository.identifierIsUnique(generatedIdentifier)
      .flatMap(unique => if (unique) Future(generatedIdentifier) else generateUniqueIdentifier(generatorFunction, count + 1))
  }

  private def generateEmpRef: Future[String] = generateUniqueIdentifier(() => { employerReferenceGenerator.sample.get.toString() })
  private def generateSaUtr: Future[String] = generateUniqueIdentifier(() => { defaultGenerator.next })
  private def generateNino: Future[String] = generateUniqueIdentifier(() => { ninoGenerator.nextNino.value })
  private def generateCtUtr: Future[String] = generateUniqueIdentifier(() => { defaultGenerator.next })
  private def generateVrn: Future[String] = generateUniqueIdentifier(() => { Vrn(vrnGenerator.sample.get.toString).vrn })
  private def generateLisaManRefNum: Future[String] = generateUniqueIdentifier(() => { lisaManRefNumGenerator.next.lisaManagerReferenceNumber })
  private def generateMtdId: Future[String] = generateUniqueIdentifier(() => { mtdItIdGenerator.next.mtdItId })
  private def generateEoriNumber: Future[String] = generateUniqueIdentifier(() => { eoriGenerator.sample.get.value })

  private def generateSetRefNum: String = setRefNumGenerator.next
  private def generatePsaId: String = psaIdGenerator.next
  private def generateArn: Future[String] = generateUniqueIdentifier(() => { arnGenerator.next })
}

class DefaultGenerator(random: Random = new Random) extends Modulus23Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: String = {
    val randomCode = f"${random.nextInt(1000000000)}%09d"
    val checkCharacter  = calculateCheckCharacter(randomCode)
    s"$checkCharacter$randomCode"
  }
}

class ArnGenerator(random: Random = new Random) extends Modulus23Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: String = {
    val randomCode = "ARN" + f"${random.nextInt(1000000)}%07d"
    val checkCharacter  = calculateCheckCharacter(randomCode)
    s"$checkCharacter$randomCode"
  }
}

class MtdItIdGenerator(random: Random = new Random) extends Modulus23Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next = {
    val randomCode = "IT" + f"${random.nextInt(1000000000)}%011d"
    val checkCharacter = calculateCheckCharacter(randomCode)
    MtdItId(s"X$checkCharacter$randomCode")
  }
}

class LisaGenerator(random: Random = new Random) extends Modulus23Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: LisaManagerReferenceNumber = {
    val randomCode = if (random.nextBoolean()) f"${random.nextInt(999999)}%06d" else f"${random.nextInt(9999)}%04d"
    LisaManagerReferenceNumber(s"Z$randomCode")
  }
}

class SecureElectronicTransferReferenceNumberGenerator(random: Random = new Random) {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: String = randomlyChosenNext

  def randomlyChosenNext = {
    // SecureElectronicTransferReferenceNumber is 12 digits randomly chosen from List of SRNs
    val snrArray = Array(
      307703077030L, 345634569999L, 376060300996L,
      111122224013L, 111122224011L, 111122224008L,
      111122223356L, 111111111199L, 111111111189L,
      111111111198L, 123456789999L, 333156333416L,
      309105308354L, 340961904502L
    )
    s"${snrArray(random.nextInt(snrArray.length))}"
  }

  def randomlyGeneratedNext: String = {
    // SecureElectronicTransferReferenceNumber must be 12 digit number not beginning with 0
    val initialDigit = random.nextInt(9) + 1    //bug random.nextInt(8) -> random.nextInt(9)
    val remainingDigits = f"${random.nextInt(Int.MaxValue)}%011d"
    s"$initialDigit$remainingDigits"
  }
}


class PensionSchemeAdministratorIdentifierGenerator(random: Random = new Random) {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: String = {
    // PensionSchemeAdministratorIdentifier must conform to this regex: ^[Aa]{1}[0-9]{7} e.g. A1234567
    val initialCharacter = if (random.nextBoolean()) "A" else "a"
    val remainingDigits = (for (i <- 1 to 7) yield random.nextInt(9)).mkString("")
    s"$initialCharacter$remainingDigits"
  }
}

object VrnChecksum {

  def apply(s: String) = s + calcCheckSum97(weightedTotal(s))

  def isValid(vrn: String): Boolean = {
    if (regexCheck(vrn)) {
      val total = weightedTotal(vrn)
      val checkSumPart = takeCheckSumPart(vrn)
      if (checkSumPart == calcCheckSum97(total).toInt) true
      else checkSumPart == calcCheckSum9755(total)
    } else false
  }

  private def calcCheckSum97(total: Int): String = {
    val x = total % 97 - 97
    f"${Math.abs(x)}%02d"
  }

  private def weightedTotal(reference: String): Int = {
    val weighting = List(8, 7, 6, 5, 4, 3, 2)
    val ref = reference.map(_.asDigit).take(7)
    (ref, weighting).zipped.map(_ * _).sum
  }

  private def calcCheckSum9755(total: Int): Int = calcCheckSum97(total + 55).toInt

  private def takeCheckSumPart(vrn: String): Int = vrn.takeRight(2).toInt

  private def regexCheck(vrn: String): Boolean = vrn.matches("[0-9]{9}")

}
