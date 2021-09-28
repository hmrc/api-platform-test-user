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

package uk.gov.hmrc.testuser.services

import com.typesafe.config.ConfigFactory
import org.joda.time.LocalDate
import org.scalacheck.Gen
import org.scalatest.enablers.{Definition, Emptiness}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.testuser.models.ServiceKeys._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserRepository

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.testuser.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.domain.Nino

trait GeneratorProvider {

  val config = ConfigFactory.parseString(
    """randomiser {
      |  individualDetails {
      |    firstName = [
      |      "Adrian"
      |    ]
      |
      |    lastName = [
      |      "Adams"
      |    ]
      |
      |    dateOfBirth = [
      |      "1940-10-10"
      |    ]
      |  }
      |
      |  address {
      |    line1 = [
      |      "1 Abbey Road"
      |    ]
      |
      |    line2 = [
      |      "Aberdeen"
      |    ]
      |
      |    postcode = [
      |      "TS1 1PA"
      |    ]
      |  }
      |}
      |""".stripMargin)

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def repository: TestUserRepository

  def generator: Generator = new Generator(repository, config)

  val eoriGenerator = Gen.listOfN(12, Gen.numChar).map("GB" + _.mkString).map(EoriNumber.apply)

  val ninoGenerator = Gen.listOfN(6, Gen.numChar).map("PE" + _.mkString + "A")
}

class GeneratorSpec extends AsyncHmrcSpec with ScalaCheckPropertyChecks {
  trait Setup extends GeneratorProvider {
    val repository = mock[TestUserRepository]
    val underTest  = generator
  }

  trait Checker {
    def check[T](attribute: T, isDefined: Boolean)(implicit definition: Definition[T], emptiness: Emptiness[T]) = {
      if(isDefined) attribute shouldBe defined
      else attribute shouldBe empty
    }
  }

  "whenF" should {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    val allKeys = Services.services.map(_.key)

    "return None when no keys match" in {
      await(
        Generator.whenF(allKeys)(Seq.empty)(Future.successful("Oops"))
      ) shouldBe None
    }

    "return None when the one key does not match" in {
      val key :: mostKeys = allKeys
      await(
        Generator.whenF(mostKeys)(Seq(key))(Future.successful("Oops"))
      ) shouldBe None
    }

    "return Some when all keys match" in {
      await(
        Generator.whenF(allKeys)(allKeys)(Future.successful("Yeah"))
      ) shouldBe Some("Yeah")
    }

    "return Some when a key matches" in {
      await(
        Generator.whenF(allKeys)(Seq(allKeys.head))(Future.successful("Yeah"))
      ) shouldBe Some("Yeah")
    }
  }

  "when" should {
    val allKeys = Services.services.map(_.key)

    "return None when no keys match" in {
      Generator.when(allKeys)(Seq.empty)("Oops") shouldBe None
    }

    "return None when the one key does not match" in {
      val key :: mostKeys = allKeys
      Generator.when(mostKeys)(Seq(key))("Oops") shouldBe None
    }

    "return Some when all keys match" in {
      Generator.when(allKeys)(allKeys)("Yeah") shouldBe Some("Yeah")
    }

    "return Some when a key matches" in {
      Generator.when(allKeys)(Seq(allKeys.head))("Yeah") shouldBe Some("Yeah")
    }
  }

  "generateTestIndividual" should {

    implicit def individualChecker(individual: TestIndividual) = new Checker {
      def shouldHave(ninoDefined: Boolean = false, vrnDefined: Boolean = false, saUtrDefined: Boolean = false, mtdItIdDefined: Boolean = false,
                     eoriDefined: Boolean = false) = {

        check(individual.nino, ninoDefined)
        check(individual.saUtr, saUtrDefined)
        check(individual.mtdItId, mtdItIdDefined)
        check(individual.eoriNumber, eoriDefined)
        check(individual.vrn, vrnDefined)
      }
    }

    "generate a NINO and MTD IT ID when MTD_INCOME_TAX service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual = await(underTest.generateTestIndividual(Seq(MTD_INCOME_TAX), None, None))
      individual shouldHave(mtdItIdDefined = true, ninoDefined = true)
    }

    "generate a NINO when NATIONAL_INSURANCE service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual = await(underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE), None, None))

      individual shouldHave(ninoDefined = true)
    }

    "use provided Nino when NATIONAL_INSURANCE or MTD_INCOME_TAX service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))
      
      val nino = ninoGenerator.sample.get

      val individual = await(underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE, MTD_INCOME_TAX), None, Some(Nino(nino))))

      individual shouldHave(mtdItIdDefined = true, ninoDefined = true)
      individual.nino shouldBe Some(nino)
    }

    "generate a SA UTR when SELF_ASSESSMENT service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual = await(underTest.generateTestIndividual(Seq(SELF_ASSESSMENT), None, None))

      individual shouldHave(saUtrDefined = true)
    }

    "generate an EORI when CUSTOMS_SERVICES service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual = await(underTest.generateTestIndividual(Seq(CUSTOMS_SERVICES), None, None))

      individual shouldHave(eoriDefined = true)
    }

    "use provided EORI when CUSTOMS_SERVICES service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))
      val eori = eoriGenerator.sample.get

      val individual = await(underTest.generateTestIndividual(Seq(CUSTOMS_SERVICES), Some(eori), None))

      individual shouldHave(eoriDefined = true)
      individual.eoriNumber shouldBe Some(eori.value)
    }

    "generate an EORI when CTC service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual = await(underTest.generateTestIndividual(Seq(CTC), None, None))

      individual shouldHave(eoriDefined = true)
    }

    "use provided EORI when CTC service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))
      val eori = eoriGenerator.sample.get

      val individual = await(underTest.generateTestIndividual(Seq(CTC), Some(eori), None))

      individual shouldHave(eoriDefined = true)
      individual.eoriNumber shouldBe Some(eori.value)
    }

    "generate an EORI when GOODS_VEHICLE_MOVEMENTS service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual =
        await(underTest.generateTestIndividual(Seq(GOODS_VEHICLE_MOVEMENTS), None, None))

      individual shouldHave (eoriDefined = true)
    }

    "use provided EORI when GOODS_VEHICLE_MOVEMENTS service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))
      val eori = eoriGenerator.sample.get

      val individual = await(underTest.generateTestIndividual(Seq(GOODS_VEHICLE_MOVEMENTS), Some(eori), None))

      individual shouldHave(eoriDefined = true)
      individual.eoriNumber shouldBe Some(eori.value)
    }

    "generate individualDetails from the configuration file" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual = await(underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX), None, None))

      individual.individualDetails shouldBe IndividualDetails("Adrian", "Adams", LocalDate.parse("1940-10-10"),
        Address("1 Abbey Road", "Aberdeen", "TS1 1PA"))
    }

    "generate a VRN when MTD_VAT service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual = await(underTest.generateTestIndividual(Seq(MTD_VAT), None, None))

      individual shouldHave(vrnDefined = true)
    }

    "set the userFullName and emailAddress" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual = await(underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX), None, None))

      individual.userFullName shouldBe s"${individual.individualDetails.firstName} ${individual.individualDetails.lastName}"

      individual.emailAddress shouldBe s"${individual.individualDetails.firstName}.${individual.individualDetails.lastName}@example.com".toLowerCase
    }

    "regenerate SA UTR if it is a duplicate" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(false), Future(true))

      val individual = await(underTest.generateTestIndividual(Seq(SELF_ASSESSMENT), None, None))

      individual shouldHave(saUtrDefined = true)
      verify(repository, times(2)).identifierIsUnique(any[String])
    }

    "regenerate NINO if it is a duplicate" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(false), Future(true))

      val individual = await(underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE), None, None))

      individual shouldHave(ninoDefined = true)
      verify(repository, times(2)).identifierIsUnique(any[String])
    }
  }

  "generateTestOrganisation" should {

    implicit def organisationChecker(org: TestOrganisation) = new Checker {
      def shouldHave(vrnDefined: Boolean = false, ninoDefined: Boolean = false, mtdItIdDefined: Boolean = false,
                     empRefDefined: Boolean = false, ctUtrDefined: Boolean = false, saUtrDefined: Boolean = false,
                     lisaManRefNumDefined: Boolean = false, secureElectronicTransferReferenceNumberDefined: Boolean = false,
                     pensionSchemeAdministratorIdentifierDefined: Boolean = false, eoriDefined: Boolean = false,
                     crnDefined: Boolean = false, taxpayerTypeDefined: Boolean = false) = {

        check(org.vrn, vrnDefined)
        check(org.nino, ninoDefined)
        check(org.mtdItId, mtdItIdDefined)
        check(org.empRef, empRefDefined)
        check(org.ctUtr, ctUtrDefined)
        check(org.saUtr, saUtrDefined)
        check(org.lisaManRefNum, lisaManRefNumDefined)
        check(org.secureElectronicTransferReferenceNumber, secureElectronicTransferReferenceNumberDefined)
        check(org.pensionSchemeAdministratorIdentifier, pensionSchemeAdministratorIdentifierDefined)
        check(org.eoriNumber, eoriDefined)
        check(org.crn, crnDefined)
        check(org.taxpayerType, taxpayerTypeDefined)
      }
    }

    "generate a NINO and MTD IT ID when MTD_INCOME_TAX service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(MTD_INCOME_TAX), None, None, None))

      org shouldHave(mtdItIdDefined = true, ninoDefined = true)
    }

    "generate a NINO when NATIONAL_INSURANCE service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(NATIONAL_INSURANCE), None, None, None))

      org shouldHave(ninoDefined = true)
    }

    "use provided Nino when NATIONAL_INSURANCE or MTD_INCOME_TAX service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))
      
      val nino = ninoGenerator.sample.get

      val individual = await(underTest.generateTestOrganisation(Seq(NATIONAL_INSURANCE, MTD_INCOME_TAX), None, Some(Nino(nino)), None))

      individual shouldHave(mtdItIdDefined = true, ninoDefined = true)
      individual.nino shouldBe Some(nino)
    }

    "generate a EMPREF when PAYE_FOR_EMPLOYERS service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(PAYE_FOR_EMPLOYERS), None, None, None))

      org shouldHave(empRefDefined = true)
    }

    "generate a CT UTR and CRN when CORPORATION_TAX service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(CORPORATION_TAX), None, None, None))

      org shouldHave(ctUtrDefined = true, crnDefined = true)
    }

    "generate a SA UTR when SELF_ASSESSMENT service is included and taxpayerType defaults" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(SELF_ASSESSMENT), None, None, None))

      org shouldHave(saUtrDefined = true, taxpayerTypeDefined = true)
    }

    "generate a SA UTR and Individual Taxpayer when SELF_ASSESSMENT and taxpayerType is provided" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(SELF_ASSESSMENT), None, None, Some(TaxpayerType("Individual"))))

      org shouldHave(saUtrDefined = true, taxpayerTypeDefined = true)
    }

    "generate a SA UTR and Partnership Taxpayer when SELF_ASSESSMENT and taxpayerType is provided" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(SELF_ASSESSMENT), None, None, Some(TaxpayerType("Partnership"))))

      org shouldHave(saUtrDefined = true, taxpayerTypeDefined = true)
    }

    "generate a VRN when SUBMIT_VAT_RETURNS service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(SUBMIT_VAT_RETURNS), None, None, None))

      org shouldHave(vrnDefined = true)
    }

    "generate a VRN when MTD_VAT service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(MTD_VAT), None, None, None))

      org shouldHave(vrnDefined = true)
    }

    "generate a lisaManagerReferenceNumber when LISA service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(LISA), None, None, None))

      org shouldHave(lisaManRefNumDefined = true)
    }

    "generate a secureElectronicTransferReferenceNumber when SECURE_ELECTRONIC_TRANSFER service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(SECURE_ELECTRONIC_TRANSFER), None, None, None))

      org shouldHave(secureElectronicTransferReferenceNumberDefined = true)
    }

    "generate a pensionSchemeAdministratorIdentifier when RELIEF_AT_SOURCE service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(RELIEF_AT_SOURCE), None, None, None))

      org shouldHave(pensionSchemeAdministratorIdentifierDefined = true)
    }

    "generate an EORI when CUSTOMS_SERVICES service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(CUSTOMS_SERVICES), None, None, None))

      org shouldHave(eoriDefined = true)
    }

    "use provided EORI when CUSTOMS_SERVICES service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))
      val eori = eoriGenerator.sample.get

      val org = await(underTest.generateTestOrganisation(Seq(CUSTOMS_SERVICES), Some(eori), None, None))

      org shouldHave(eoriDefined = true)
      org.eoriNumber shouldBe Some(eori.value)
    }

    "generate an EORI when CTC service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(CTC), None, None, None))

      org shouldHave(eoriDefined = true)
    }

    "use provided EORI when legacy CTC service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))
      val eori = eoriGenerator.sample.get

      val org = await(underTest.generateTestOrganisation(Seq(CTC_LEGACY), Some(eori), None, None))

      org shouldHave(eoriDefined = true)
      org.eoriNumber shouldBe Some(eori.value)
    }

    "generate an EORI when GOODS_VEHICLE_MOVEMENTS service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(GOODS_VEHICLE_MOVEMENTS), None, None, None))

      org shouldHave (eoriDefined = true)
    }

    "use provided EORI when GOODS_VEHICLE_MOVEMENTS service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))
      val eori = eoriGenerator.sample.get

      val org = await(underTest.generateTestOrganisation(Seq(GOODS_VEHICLE_MOVEMENTS), Some(eori), None, None))

      org shouldHave (eoriDefined = true)
      org.eoriNumber shouldBe Some(eori.value)
    }

    "generate an EORI when SAFETY_AND_SECURITY service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(SAFETY_AND_SECURITY), None, None, None))

      org shouldHave(eoriDefined = true)
    }

    "use provided EORI when SAFETY_AND_SECURITY service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))
      val eori = eoriGenerator.sample.get

      val org = await(underTest.generateTestOrganisation(Seq(SAFETY_AND_SECURITY), Some(eori), None, None))

      org shouldHave (eoriDefined = true)
      org.eoriNumber shouldBe Some(eori.value)
    }

    "do not generate EORI when none of CUSTOMS_SERVICE, GOODS_VEHICLE_MOVEMENTS, CTC, SAFETY_AND_SECURITY service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))
      val eoriToIgnore = eoriGenerator.sample.get

      val org = await(underTest.generateTestOrganisation(Seq.empty, Some(eoriToIgnore), None, None))

      org shouldHave (eoriDefined = false)
    }

    "set the userFullName and emailAddress" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val organisation = await(underTest.generateTestOrganisation(Seq(MTD_INCOME_TAX), None, None, None))

      organisation.userFullName.matches("[a-zA-Z]+ [a-zA-Z]+") shouldBe true

      val nameParts = organisation.userFullName.split(" ")

      organisation.emailAddress shouldBe s"${nameParts(0)}.${nameParts(1)}@example.com".toLowerCase
    }

    "regenerate VRN if it is a duplicate" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(false), Future(true))

      val organisation = await(underTest.generateTestOrganisation(Seq(SUBMIT_VAT_RETURNS), None, None, None))

      organisation shouldHave(vrnDefined = true)
      verify(repository, times(2)).identifierIsUnique(any[String])
    }

    "regenerate Employer Reference if it is a duplicate" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(false), Future(true))

      val organisation = await(underTest.generateTestOrganisation(Seq(PAYE_FOR_EMPLOYERS), None, None, None))

      organisation shouldHave(empRefDefined = true)
      verify(repository, times(2)).identifierIsUnique(any[String])
    }
  }

  "CompanyReferenceNumberGenerator generates valid Crn" in {
    val crnRegex = "^[A-Z0-9]{1,10}$".r
    val result = new CompanyReferenceNumberGenerator().next

    result.matches(crnRegex.toString()) shouldBe true
  }

  "generateTestAgent" should {
    "not generate any identifiers when no services are included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val agent = await(underTest.generateTestAgent(Seq.empty))

      agent.arn shouldBe None
    }

    "generate an agent reference number when AGENT_SERVICES service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val agent = await(underTest.generateTestAgent(Seq(AGENT_SERVICES)))

      agent.arn shouldBe defined
    }

    "set the userFullName and emailAddress" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val agent = await(underTest.generateTestAgent(Seq(AGENT_SERVICES)))

      agent.userFullName.matches("[a-zA-Z]+ [a-zA-Z]+") shouldBe true

      val nameParts = agent.userFullName.split(" ")

      agent.emailAddress shouldBe s"${nameParts(0)}.${nameParts(1)}@example.com".toLowerCase
    }
  }

  "VrnChecksum" should {
    "generate valid VRN checksum" in new Setup {
      forAll(Gen.choose(1000000, 1999999)) { vrnBase =>
        val vrn  = VrnChecksum.apply(vrnBase.toString)
        VrnChecksum.isValid(vrn) shouldBe true
      }
    }

    "validate VRN" in new Setup {
      VrnChecksum.isValid("666000754") shouldBe true
      VrnChecksum.isValid("666163716") shouldBe true
      VrnChecksum.isValid("666541906") shouldBe true
      VrnChecksum.isValid("666163716") shouldBe true
      VrnChecksum.isValid("666634014") shouldBe true
      VrnChecksum.isValid("666159897") shouldBe true
      VrnChecksum.isValid("666159896") shouldBe false
      VrnChecksum.isValid("66159896 ") shouldBe false
      VrnChecksum.isValid("") shouldBe false
      VrnChecksum.isValid("000000000") shouldBe false
      VrnChecksum.isValid(" 666159896") shouldBe false
    }
  }
}
