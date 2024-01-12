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

package uk.gov.hmrc.testuser.services

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

import com.typesafe.config.Config
import org.scalacheck.Gen

import uk.gov.hmrc.domain._

import uk.gov.hmrc.testuser.models.ServiceKey._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.util.Randomiser

object Generator {

  def whenF[T](services: Seq[ServiceKey])(keys: Seq[ServiceKey])(thenDo: => Future[T])(implicit ec: ExecutionContext): Future[Option[T]] = {
    if (services.intersect(keys).isEmpty) {
      Future.successful(None)
    } else {
      thenDo.map(Some.apply)
    }
  }

  def whenElseF[T](
      services: Seq[ServiceKey]
    )(
      ifKeys: Seq[ServiceKey]
    )(
      thenDo: => Future[T]
    )(
      elseKeys: Seq[ServiceKey]
    )(
      elseDo: => Future[T]
    )(implicit ec: ExecutionContext
    ): Future[Option[T]] = {
    if (services.intersect(ifKeys).nonEmpty) {
      thenDo.map(Some.apply)
    } else if (services.intersect(elseKeys).nonEmpty) {
      elseDo.map(Some.apply)
    } else {
      Future.successful(None)
    }
  }

  def when[T](services: Seq[ServiceKey])(keys: Seq[ServiceKey])(thenDo: => T): Option[T] = {
    if (services.intersect(keys).isEmpty) {
      None
    } else {
      Some(thenDo)
    }
  }
}

@Singleton
class Generator @Inject() (val testUserRepository: TestUserRepository, val config: Config)(implicit ec: ExecutionContext)
    extends Randomiser
    with ApplicationLogger {

  private val userIdGenerator   = Gen.listOfN(12, Gen.numChar).map(_.mkString)
  private val passwordGenerator = Gen.listOfN(12, Gen.alphaNumChar).map(_.mkString)
  private val utrGenerator      = new UtrGenerator()
  private val ninoGenerator     = new uk.gov.hmrc.domain.Generator()

  private val employerReferenceGenerator: Gen[EmpRef] = for {
    taxOfficeNumber    <- Gen.choose(100, 999).map(x => x.toString)
    taxOfficeReference <- Gen.listOfN(10, Gen.alphaNumChar).map(_.mkString.toUpperCase)
  } yield EmpRef.fromIdentifiers(s"$taxOfficeNumber/$taxOfficeReference")

  private val vrnGenerator: Gen[String] = Gen.choose(1000000, 9999999).map(i => VrnChecksum.apply(i.toString)).retryUntil(VrnChecksum.isValid)
  private val mtdItIdGenerator          = new MtdItIdGenerator()
  private val lisaManRefNumGenerator    = new LisaGenerator()
  private val setRefNumGenerator        = new SecureElectronicTransferReferenceNumberGenerator()
  private val psaIdGenerator            = new PensionSchemeAdministratorIdentifierGenerator()
  private val eoriGenerator             = Gen.listOfN(12, Gen.numChar).map("GB" + _.mkString).map(EoriNumber.apply)

  private val exciseNumberGenerator = for {
    firstPart  <- Gen.listOfN(2, Gen.alphaUpperChar).map(_.mkString)
    secondPart <- Gen.listOfN(11, Gen.alphaNumChar).map(_.mkString)
  } yield EoriNumber(s"$firstPart$secondPart")
  private val arnGenerator          = new ArnGenerator()
  private val crnGenerator          = new CompanyReferenceNumberGenerator()

  private val agentCodeGenerator = Gen.listOfN(10, Gen.numChar).map(_.mkString)

  def useProvidedOrGenerateEoriNumber(eoriNumber: Option[EoriNumber], forEMCS: Boolean = false): Future[String] = {
    val generator = if (forEMCS) generateExciseNumber else generateEoriNumber
    eoriNumber.fold(generator)(provided => Future.successful(provided.value))
  }

  def useProvidedOrGeneratedNino(nino: Option[Nino]): Future[String] = {
    nino.fold(generateNino)(providedNino => Future.successful(providedNino.value))
  }

  def useProvidedTaxpayerType(maybeString: Option[TaxpayerType]): Future[String] =
    Future.successful(maybeString.fold("Individual")(provided => provided.value))

  def generateTestIndividual(services: Seq[ServiceKey] = Seq.empty, eoriNumber: Option[EoriNumber], nino: Option[Nino]): Future[TestIndividual] = {
    def whenF[T](keys: ServiceKey*)(thenDo: => Future[T]): Future[Option[T]] = Generator.whenF(services)(keys)(thenDo)

    for {
      saUtr              <- whenF(SELF_ASSESSMENT)(generateSaUtr)
      nino               <- whenF(NATIONAL_INSURANCE, MTD_INCOME_TAX)(useProvidedOrGeneratedNino(nino))
      mtdItId            <- whenF(MTD_INCOME_TAX)(generateMtdId)
      eoriNumber         <- whenF(CUSTOMS_SERVICES, CTC_LEGACY, CTC, GOODS_VEHICLE_MOVEMENTS)(useProvidedOrGenerateEoriNumber(eoriNumber))
      vrn                <- whenF(MTD_VAT)(generateVrn)
      vatRegistrationDate = vrn.map(_ => LocalDate.now.minusYears(Gen.chooseNum(1, 20).sample.get))
      groupIdentifier     = Some(generateGroupIdentifier)
      individualDetails   = generateIndividualDetails
      userFullName        = generateUserFullName(individualDetails.firstName, individualDetails.lastName)
      emailAddress        = generateEmailAddress(individualDetails.firstName, individualDetails.lastName)
    } yield {
      val props = Map[TestUserPropKey, Option[String]](
        TestUserPropKey.saUtr           -> saUtr,
        TestUserPropKey.nino            -> nino,
        TestUserPropKey.mtdItId         -> mtdItId,
        TestUserPropKey.vrn             -> vrn,
        TestUserPropKey.eoriNumber      -> eoriNumber,
        TestUserPropKey.groupIdentifier -> groupIdentifier
      ).collect {
        case (key, Some(value)) => key -> value
      }
      TestIndividual(
        generateUserId,
        generatePassword,
        userFullName,
        emailAddress,
        individualDetails,
        services,
        vatRegistrationDate,
        props
      )
    }
  }

  def generateTestOrganisation(
      services: Seq[ServiceKey] = Seq.empty,
      eoriNumber: Option[EoriNumber],
      nino: Option[Nino],
      taxpayerType: Option[TaxpayerType]
    ): Future[TestOrganisation] = {

    def whenF[T](keys: ServiceKey*)(thenDo: => Future[T]): Future[Option[T]] = Generator.whenF(services)(keys)(thenDo)

    def whenElseF[T](ifKeys: ServiceKey*)(thenDo: => Future[T])(elseKeys: ServiceKey*)(elseDo: => Future[T]): Future[Option[T]] =
      Generator.whenElseF(services)(ifKeys)(thenDo)(elseKeys)(elseDo)

    def when[T](keys: ServiceKey*)(thenDo: => T): Option[T] = Generator.when(services)(keys)(thenDo)

    for {
      saUtr              <- whenF(SELF_ASSESSMENT)(generateSaUtr)
      nino               <- whenF(NATIONAL_INSURANCE, MTD_INCOME_TAX)(useProvidedOrGeneratedNino(nino))
      mtdItId            <- whenF(MTD_INCOME_TAX)(generateMtdId)
      empRef             <- whenF(PAYE_FOR_EMPLOYERS)(generateEmpRef)
      ctUtr              <- whenF(CORPORATION_TAX)(generateCtUtr)
      vrn                <- whenF(SUBMIT_VAT_RETURNS, MTD_VAT)(generateVrn)
      vatRegistrationDate = vrn.map(_ => LocalDate.now.minusYears(Gen.chooseNum(1, 20).sample.get))
      lisaManRefNum      <- whenF(LISA)(generateLisaManRefNum)
      setRefNum           = when(SECURE_ELECTRONIC_TRANSFER)(generateSetRefNum)
      psaId               = when(RELIEF_AT_SOURCE)(generatePsaId)
      eoriNumber         <- whenElseF(CUSTOMS_SERVICES, CTC_LEGACY, CTC, SAFETY_AND_SECURITY, GOODS_VEHICLE_MOVEMENTS)(useProvidedOrGenerateEoriNumber(eoriNumber))(EMCS)(
                              useProvidedOrGenerateEoriNumber(eoriNumber, forEMCS = true)
                            )
      groupIdentifier     = Some(generateGroupIdentifier)
      firstName           = generateFirstName
      lastName            = generateLastName
      userFullName        = generateUserFullName(firstName, lastName)
      emailAddress        = generateEmailAddress(firstName, lastName)
      organisationDetails = generateOrganisationDetails
      individualDetails   = Some(generateIndividualDetails(firstName, lastName))
      companyRegNo       <- whenF(CORPORATION_TAX)(generateCrn)
      taxpayerType       <- whenF(SELF_ASSESSMENT)(useProvidedTaxpayerType(taxpayerType).map(maybeVal => maybeVal.trim))

    } yield {
      val props = Map[TestUserPropKey, Option[String]](
        TestUserPropKey.saUtr                                   -> saUtr,
        TestUserPropKey.nino                                    -> nino,
        TestUserPropKey.mtdItId                                 -> mtdItId,
        TestUserPropKey.empRef                                  -> empRef,
        TestUserPropKey.ctUtr                                   -> ctUtr,
        TestUserPropKey.vrn                                     -> vrn,
        TestUserPropKey.lisaManRefNum                           -> lisaManRefNum,
        TestUserPropKey.secureElectronicTransferReferenceNumber -> setRefNum,
        TestUserPropKey.pensionSchemeAdministratorIdentifier    -> psaId,
        TestUserPropKey.eoriNumber                              -> eoriNumber,
        TestUserPropKey.groupIdentifier                         -> groupIdentifier,
        TestUserPropKey.crn                                     -> companyRegNo,
        TestUserPropKey.taxpayerType                            -> taxpayerType
      ).collect {
        case (key, Some(value)) => key -> value
      }
      TestOrganisation(
        generateUserId,
        generatePassword,
        userFullName,
        emailAddress,
        organisationDetails,
        individualDetails,
        services,
        vatRegistrationDate,
        props
      )
    }
  }

  def generateTestAgent(services: Seq[ServiceKey] = Seq.empty): Future[TestAgent] = {
    def whenAppropriate: (=> Future[String]) => Future[Option[String]] = gen =>
      if (services.contains(AGENT_SERVICES)) {
        gen.map(Some(_))
      } else {
        Future.successful(None)
      }

    for {
      arn            <- whenAppropriate(generateArn)
      agentCode      <- whenAppropriate(generateAgentCode)
      firstName       = generateFirstName
      lastName        = generateLastName
      userFullName    = generateUserFullName(firstName, lastName)
      emailAddress    = generateEmailAddress(firstName, lastName)
      groupIdentifier = Some(generateGroupIdentifier)
    } yield {
      val props = Map[TestUserPropKey, Option[String]](
        TestUserPropKey.groupIdentifier -> groupIdentifier,
        TestUserPropKey.arn             -> arn,
        TestUserPropKey.agentCode       -> agentCode
      ).collect {
        case (key, Some(value)) => key -> value
      }
      TestAgent(generateUserId, generatePassword, userFullName, emailAddress, services, props)
    }
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

  private def generateIndividualDetails(firstName: String, lastName: String): IndividualDetails = {
    IndividualDetails(
      firstName,
      lastName,
      LocalDate.parse(randomConfigString("randomiser.individualDetails.dateOfBirth")),
      generateAddress()
    )
  }

  private def generateIndividualDetails: IndividualDetails = {
    generateIndividualDetails(
      generateFirstName,
      generateLastName
    )
  }

  private def generateOrganisationDetails = {
    val randomOrganisationName = "Company " + Gen.listOfN(6, Gen.alphaNumChar).map(_.mkString.toUpperCase).sample.get
    OrganisationDetails(randomOrganisationName, generateAddress())
  }

  private def generateUserId = userIdGenerator.sample.get

  private def generateGroupIdentifier = userIdGenerator.sample.get

  private def generatePassword = passwordGenerator.sample.get

  private def generateUniqueIdentifier[T <: String](propKey: TestUserPropKey)(generatorFunction: () => T, count: Int = 1)(implicit ec: ExecutionContext): Future[T] = {
    logger.info(s"Generating tax identifier attempt $count")
    val generatedIdentifier = generatorFunction()
    testUserRepository.identifierIsUnique(propKey)(generatedIdentifier)
      .flatMap(unique => if (unique) Future(generatedIdentifier) else generateUniqueIdentifier(propKey)(generatorFunction, count + 1))
  }

  private def generateEmpRef: Future[String] = generateUniqueIdentifier(TestUserPropKey.empRef)(() => { employerReferenceGenerator.sample.get.toString })
  private def generateSaUtr: Future[String]  = generateUniqueIdentifier(TestUserPropKey.saUtr)(() => { utrGenerator.next })
  private def generateNino: Future[String]   = generateUniqueIdentifier(TestUserPropKey.nino)(() => { ninoGenerator.nextNino.value })
  private def generateCtUtr: Future[String]  = generateUniqueIdentifier(TestUserPropKey.ctUtr)(() => { utrGenerator.next })
  private def generateVrn: Future[String]    = generateUniqueIdentifier(TestUserPropKey.vrn)(() => { Vrn(vrnGenerator.sample.get).vrn })

  private def generateCrn: Future[String] = generateUniqueIdentifier(TestUserPropKey.crn)(() => {
    crnGenerator.next
  })

  private def generateLisaManRefNum: Future[String] = generateUniqueIdentifier(TestUserPropKey.lisaManRefNum)(() => {
    lisaManRefNumGenerator.next.lisaManagerReferenceNumber
  })

  private def generateMtdId: Future[String] = generateUniqueIdentifier(TestUserPropKey.mtdItId)(() => {
    mtdItIdGenerator.next.mtdItId
  })

  private def generateEoriNumber: Future[String] = generateUniqueIdentifier(TestUserPropKey.eoriNumber)(() => {
    eoriGenerator.sample.get.value
  })

  private def generateExciseNumber: Future[String] = generateUniqueIdentifier(TestUserPropKey.eoriNumber)(() => {
    exciseNumberGenerator.sample.get.value
  })

  private def generateAgentCode: Future[String] = generateUniqueIdentifier(TestUserPropKey.agentCode)(() => {
    agentCodeGenerator.sample.get
  })

  private def generateSetRefNum: String = setRefNumGenerator.next
  private def generatePsaId: String     = psaIdGenerator.next

  private def generateArn: Future[String] = generateUniqueIdentifier(TestUserPropKey.arn)(() => {
    arnGenerator.next
  })
}

class UtrGenerator(random: Random = new Random) extends Modulus11Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: String = {
    val randomCode     = f"${random.nextInt(1000000000)}%09d"
    val checkCharacter = calculateCheckCharacter(randomCode)
    s"$checkCharacter$randomCode"
  }
}

class ArnGenerator(random: Random = new Random) extends Modulus23Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: String = {
    val randomCode     = "ARN" + f"${random.nextInt(1000000)}%07d"
    val checkCharacter = calculateCheckCharacter(randomCode)
    s"$checkCharacter$randomCode"
  }
}

class MtdItIdGenerator(random: Random = new Random) extends Modulus23Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: MtdItId = {
    val randomCode     = "IT" + f"${random.nextInt(1000000000)}%011d"
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
    val initialDigit    = random.nextInt(9) + 1 // bug random.nextInt(8) -> random.nextInt(9)
    val remainingDigits = f"${random.nextInt(Int.MaxValue)}%011d"
    s"$initialDigit$remainingDigits"
  }
}

class PensionSchemeAdministratorIdentifierGenerator(random: Random = new Random) {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def next: String = {
    // PensionSchemeAdministratorIdentifier must conform to this regex: ^[Aa]{1}[0-9]{7} e.g. A1234567
    val initialCharacter = if (random.nextBoolean()) "A" else "a"
    val remainingDigits  = (for (i <- 1 to 7) yield random.nextInt(9)).mkString("")
    s"$initialCharacter$remainingDigits"
  }
}

class CompanyReferenceNumberGenerator(random: Random = new Random) {
  private val maxNum = 9
  private val length = 10

  def next: String = (for (_ <- 1 to length) yield random.nextInt(maxNum)).mkString("")
}

object VrnChecksum {

  def apply(s: String) = s + calcCheckSum97(weightedTotal(s))

  def isValid(vrn: String): Boolean = {
    if (regexCheck(vrn)) {
      val total        = weightedTotal(vrn)
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
    val ref       = reference.map(_.asDigit).take(7)
    ref.lazyZip(weighting).map(_ * _).sum
  }

  private def calcCheckSum9755(total: Int): Int = calcCheckSum97(total + 55).toInt

  private def takeCheckSumPart(vrn: String): Int = vrn.takeRight(2).toInt

  private def regexCheck(vrn: String): Boolean = vrn.matches("[0-9]{9}")

}
