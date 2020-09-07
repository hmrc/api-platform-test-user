/*
 * Copyright 2020 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Gen
import org.scalatest.enablers.{Definition, Emptiness}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.models.ServiceKeys._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

trait GeneratorProvider {

  val config = ConfigFactory.parseString("""randomiser {
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

  implicit val ec: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  def repository: TestUserRepository

  def generator: Generator = new Generator(repository, config)
}

class GeneratorSpec extends UnitSpec with MockitoSugar with PropertyChecks {

  trait Setup extends GeneratorProvider {

    val repository = mock[TestUserRepository]

    val underTest = generator
  }

  trait Checker {
    def check[T](attribute: T, isDefined: Boolean)(
        implicit definition: Definition[T],
        emptiness: Emptiness[T]) = {
      if (isDefined) attribute shouldBe defined
      else attribute shouldBe empty
    }
  }

  "generateTestIndividual" should {

    implicit def individualChecker(individual: TestIndividual) = new Checker {
      def shouldHave(ninoDefined: Boolean = false,
                     vrnDefined: Boolean = false,
                     saUtrDefined: Boolean = false,
                     mtdItIdDefined: Boolean = false,
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

      val individual =
        await(underTest.generateTestIndividual(Seq(MTD_INCOME_TAX)))

      individual shouldHave (mtdItIdDefined = true, ninoDefined = true)
    }

    "generate a NINO when NATIONAL_INSURANCE service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual =
        await(underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE)))

      individual shouldHave (ninoDefined = true)
    }

    "generate a SA UTR when SELF_ASSESSMENT service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual =
        await(underTest.generateTestIndividual(Seq(SELF_ASSESSMENT)))

      individual shouldHave (saUtrDefined = true)
    }

    "generate an EORI when CUSTOMS_SERVICES service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual =
        await(underTest.generateTestIndividual(Seq(CUSTOMS_SERVICES)))

      individual shouldHave (eoriDefined = true)
    }

    "generate an EORI when GOODS_VEHICLE_MOVEMENTS service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual =
        await(underTest.generateTestIndividual(Seq(GOODS_VEHICLE_MOVEMENTS)))

      individual shouldHave (eoriDefined = true)
    }

    "generate an EORI when ICS_SAFETY_AND_SECURITY service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual =
        await(underTest.generateTestIndividual(Seq(ICS_SAFETY_AND_SECURITY)))

      individual shouldHave (eoriDefined = true)
    }

    "generate individualDetails from the configuration file" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual = await(
        underTest.generateTestIndividual(
          Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX)))

      individual.individualDetails shouldBe IndividualDetails(
        "Adrian",
        "Adams",
        LocalDate.parse("1940-10-10"),
        Address("1 Abbey Road", "Aberdeen", "TS1 1PA"))
    }

    "generate a VRN when MTD_VAT service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual = await(underTest.generateTestIndividual(Seq(MTD_VAT)))

      individual shouldHave (vrnDefined = true)
    }

    "set the userFullName and emailAddress" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val individual = await(
        underTest.generateTestIndividual(
          Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX)))

      individual.userFullName shouldBe s"${individual.individualDetails.firstName} ${individual.individualDetails.lastName}"

      individual.emailAddress shouldBe s"${individual.individualDetails.firstName}.${individual.individualDetails.lastName}@example.com".toLowerCase
    }

    "regenerate SA UTR if it is a duplicate" in new Setup {
      when(repository.identifierIsUnique(any[String]))
        .thenReturn(Future(false), Future(true))

      val individual =
        await(underTest.generateTestIndividual(Seq(SELF_ASSESSMENT)))

      individual shouldHave (saUtrDefined = true)
      verify(repository, times(2)).identifierIsUnique(any[String])
    }

    "regenerate NINO if it is a duplicate" in new Setup {
      when(repository.identifierIsUnique(any[String]))
        .thenReturn(Future(false), Future(true))

      val individual =
        await(underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE)))

      individual shouldHave (ninoDefined = true)
      verify(repository, times(2)).identifierIsUnique(any[String])
    }
  }

  "generateTestOrganisation" should {

    implicit def organisationChecker(org: TestOrganisation) = new Checker {
      def shouldHave(vrnDefined: Boolean = false,
                     ninoDefined: Boolean = false,
                     mtdItIdDefined: Boolean = false,
                     empRefDefined: Boolean = false,
                     ctUtrDefined: Boolean = false,
                     saUtrDefined: Boolean = false,
                     lisaManRefNumDefined: Boolean = false,
                     secureElectronicTransferReferenceNumberDefined: Boolean =
                       false,
                     pensionSchemeAdministratorIdentifierDefined: Boolean =
                       false,
                     eoriDefined: Boolean = false) = {

        check(org.vrn, vrnDefined)
        check(org.nino, ninoDefined)
        check(org.mtdItId, mtdItIdDefined)
        check(org.empRef, empRefDefined)
        check(org.ctUtr, ctUtrDefined)
        check(org.saUtr, saUtrDefined)
        check(org.lisaManRefNum, lisaManRefNumDefined)
        check(org.secureElectronicTransferReferenceNumber,
              secureElectronicTransferReferenceNumberDefined)
        check(org.pensionSchemeAdministratorIdentifier,
              pensionSchemeAdministratorIdentifierDefined)
        check(org.eoriNumber, eoriDefined)
      }
    }

    "generate a NINO and MTD IT ID when MTD_INCOME_TAX service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(MTD_INCOME_TAX)))

      org shouldHave (mtdItIdDefined = true, ninoDefined = true)
    }

    "generate a NINO when NATIONAL_INSURANCE service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org =
        await(underTest.generateTestOrganisation(Seq(NATIONAL_INSURANCE)))

      org shouldHave (ninoDefined = true)
    }

    "generate a EMPREF when PAYE_FOR_EMPLOYERS service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org =
        await(underTest.generateTestOrganisation(Seq(PAYE_FOR_EMPLOYERS)))

      org shouldHave (empRefDefined = true)
    }

    "generate a CT UTR when CORPORATION_TAX service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(CORPORATION_TAX)))

      org shouldHave (ctUtrDefined = true)
    }

    "generate a SA UTR when SELF_ASSESSMENT service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(SELF_ASSESSMENT)))

      org shouldHave (saUtrDefined = true)
    }

    "generate a VRN when SUBMIT_VAT_RETURNS service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org =
        await(underTest.generateTestOrganisation(Seq(SUBMIT_VAT_RETURNS)))

      org shouldHave (vrnDefined = true)
    }

    "generate a VRN when MTD_VAT service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(MTD_VAT)))

      org shouldHave (vrnDefined = true)
    }

    "generate a lisaManagerReferenceNumber when LISA service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(LISA)))

      org shouldHave (lisaManRefNumDefined = true)
    }

    "generate a secureElectronicTransferReferenceNumber when SECURE_ELECTRONIC_TRANSFER service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(
        underTest.generateTestOrganisation(Seq(SECURE_ELECTRONIC_TRANSFER)))

      org shouldHave (secureElectronicTransferReferenceNumberDefined = true)
    }

    "generate a pensionSchemeAdministratorIdentifier when RELIEF_AT_SOURCE service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(RELIEF_AT_SOURCE)))

      org shouldHave (pensionSchemeAdministratorIdentifierDefined = true)
    }

    "generate an EORI when CUSTOMS_SERVICES service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org = await(underTest.generateTestOrganisation(Seq(CUSTOMS_SERVICES)))

      org shouldHave (eoriDefined = true)
    }

    "generate an EORI when GOODS_VEHICLE_MOVEMENTS service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org =
        await(underTest.generateTestOrganisation(Seq(GOODS_VEHICLE_MOVEMENTS)))

      org shouldHave (eoriDefined = true)
    }

    "generate an EORI when ICS_SAFETY_AND_SECURITY service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val org =
        await(underTest.generateTestOrganisation(Seq(ICS_SAFETY_AND_SECURITY)))

      org shouldHave (eoriDefined = true)
    }

    "set the userFullName and emailAddress" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val organisation = underTest.generateTestOrganisation(Seq(MTD_INCOME_TAX))

      organisation.userFullName.matches("[a-zA-Z]+ [a-zA-Z]+") shouldBe true

      val nameParts = organisation.userFullName.split(" ")

      organisation.emailAddress shouldBe s"${nameParts(0)}.${nameParts(1)}@example.com".toLowerCase
    }

    "regenerate VRN if it is a duplicate" in new Setup {
      when(repository.identifierIsUnique(any[String]))
        .thenReturn(Future(false), Future(true))

      val organisation =
        await(underTest.generateTestOrganisation(Seq(SUBMIT_VAT_RETURNS)))

      organisation shouldHave (vrnDefined = true)
      verify(repository, times(2)).identifierIsUnique(any[String])
    }

    "regenerate Employer Reference if it is a duplicate" in new Setup {
      when(repository.identifierIsUnique(any[String]))
        .thenReturn(Future(false), Future(true))

      val organisation =
        await(underTest.generateTestOrganisation(Seq(PAYE_FOR_EMPLOYERS)))

      organisation shouldHave (empRefDefined = true)
      verify(repository, times(2)).identifierIsUnique(any[String])
    }
  }

  "generateTestAgent" should {
    "not generate any identifiers when no services are included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val agent = underTest.generateTestAgent(Seq.empty)

      agent.arn shouldBe None
    }

    "generate an agent reference number when AGENT_SERVICES service is included" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val agent = underTest.generateTestAgent(Seq(AGENT_SERVICES))

      agent.arn shouldBe defined
    }

    "set the userFullName and emailAddress" in new Setup {
      when(repository.identifierIsUnique(any[String])).thenReturn(Future(true))

      val agent = underTest.generateTestAgent(Seq(AGENT_SERVICES))

      agent.userFullName.matches("[a-zA-Z]+ [a-zA-Z]+") shouldBe true

      val nameParts = agent.userFullName.split(" ")

      agent.emailAddress shouldBe s"${nameParts(0)}.${nameParts(1)}@example.com".toLowerCase
    }
  }

  "VrnChecksum" should {
    "generate valid VRN checksum" in new Setup {
      forAll(Gen.choose(1000000, 1999999)) { vrnBase =>
        val vrn = VrnChecksum.apply(vrnBase.toString)
        VrnChecksum.isValid(vrn) shouldBe true
        println(vrn)
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
