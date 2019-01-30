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

package unit.uk.gov.hmrc.testuser.services

import org.joda.time.LocalDate
import org.scalacheck.Gen
import org.scalatest.enablers.{Definition, Emptiness}
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.prop.PropertyChecks
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.models.ServiceName._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.services.{Generator, VrnChecksum}
import unit.uk.gov.hmrc.testuser.services.CustomMatchers.haveDifferentPropertiesThan

import scala.language.implicitConversions

class GeneratorSpec extends UnitSpec with PropertyChecks {

  val underTest = new Generator {
    override val fileName = "randomiser-unique-values"
  }

  trait Checker {
    def check[T](attribute: T, isDefined: Boolean)(implicit definition: Definition[T], emptiness: Emptiness[T]) = {
      if(isDefined) attribute shouldBe defined
      else attribute shouldBe empty
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

    "create a different test individual at every run" in {
      def generate(): TestIndividual =
        underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX, CUSTOMS_SERVICES, MTD_VAT))

      val individual1 = generate()
      val individual2 = generate()

      individual1 should haveDifferentPropertiesThan(individual2)
    }

    "generate a NINO and MTD IT ID when MTD_INCOME_TAX service is included" in {
      val individual = underTest.generateTestIndividual(Seq(MTD_INCOME_TAX))

      individual shouldHave(mtdItIdDefined = true, ninoDefined = true)
    }

    "generate a NINO when NATIONAL_INSURANCE service is included" in {
      val individual = underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE))

      individual shouldHave(ninoDefined = true)
    }

    "generate a SA UTR when SELF_ASSESSMENT service is included" in {
      val individual = underTest.generateTestIndividual(Seq(SELF_ASSESSMENT))

      individual shouldHave(saUtrDefined = true)
    }

    "generate an EORI when CUSTOMS_SERVICES service is included" in {
      val individual = underTest.generateTestIndividual(Seq(CUSTOMS_SERVICES))

      individual shouldHave(eoriDefined = true)
    }

    "generate individualDetails from the configuration file" in {
      val individual = underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX))

      individual.individualDetails shouldBe IndividualDetails("Adrian", "Adams", LocalDate.parse("1940-10-10"),
        Address("1 Abbey Road", "Aberdeen", "TS1 1PA"))
    }

    "generate a VRN when MTD_VAT service is included" in {
      val individual = underTest.generateTestIndividual(Seq(MTD_VAT))

      individual shouldHave(vrnDefined = true)
    }

    "set the userFullName and emailAddress" in {
      val individual = underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX))

      individual.userFullName shouldBe s"${individual.individualDetails.firstName} ${individual.individualDetails.lastName}"

      individual.emailAddress shouldBe s"${individual.individualDetails.firstName}.${individual.individualDetails.lastName}@example.com".toLowerCase
    }
  }

  "generateTestOrganisation" should {

    implicit def organisationChecker(org: TestOrganisation) = new Checker {
      def shouldHave(vrnDefined: Boolean = false, ninoDefined: Boolean = false, mtdItIdDefined: Boolean = false,
               empRefDefined: Boolean = false, ctUtrDefined: Boolean = false, saUtrDefined: Boolean = false,
               lisaManRefNumDefined: Boolean = false, secureElectronicTransferReferenceNumberDefined: Boolean = false,
               pensionSchemeAdministratorIdentifierDefined: Boolean = false, eoriDefined: Boolean = false) = {

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
      }
    }

    "create a different test organisation at every run" in {
      def generate(): TestOrganisation =
        underTest.generateTestOrganisation(Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX, MTD_VAT,
          CORPORATION_TAX, PAYE_FOR_EMPLOYERS, SUBMIT_VAT_RETURNS, LISA, SECURE_ELECTRONIC_TRANSFER, RELIEF_AT_SOURCE,
          CUSTOMS_SERVICES))

      val organisation1 = generate()
      val organisation2 = generate()

      organisation1 should haveDifferentPropertiesThan(organisation2)
    }

    "generate a NINO and MTD IT ID when MTD_INCOME_TAX service is included" in {
      val org = underTest.generateTestOrganisation(Seq(MTD_INCOME_TAX))

      org shouldHave(mtdItIdDefined = true, ninoDefined = true)
    }

    "generate a NINO when NATIONAL_INSURANCE service is included" in {
      val org = underTest.generateTestOrganisation(Seq(NATIONAL_INSURANCE))

      org shouldHave(ninoDefined = true)
    }

    "generate a EMPREF when PAYE_FOR_EMPLOYERS service is included" in {
      val org = underTest.generateTestOrganisation(Seq(PAYE_FOR_EMPLOYERS))

      org shouldHave(empRefDefined = true)
    }

    "generate a CT UTR when CORPORATION_TAX service is included" in {
      val org = underTest.generateTestOrganisation(Seq(CORPORATION_TAX))

      org shouldHave(ctUtrDefined = true)
    }

    "generate a SA UTR when SELF_ASSESSMENT service is included" in {
      val org = underTest.generateTestOrganisation(Seq(SELF_ASSESSMENT))

      org shouldHave(saUtrDefined = true)
    }

    "generate a VRN when SUBMIT_VAT_RETURNS service is included" in {
      val org = underTest.generateTestOrganisation(Seq(SUBMIT_VAT_RETURNS))

      org shouldHave(vrnDefined = true)
    }

    "generate a VRN when MTD_VAT service is included" in {
      val org = underTest.generateTestOrganisation(Seq(MTD_VAT))

      org shouldHave(vrnDefined = true)
    }

    "generate a lisaManagerReferenceNumber when LISA service is included" in {
      val org = underTest.generateTestOrganisation(Seq(LISA))

      org shouldHave(lisaManRefNumDefined = true)
    }

    "generate a secureElectronicTransferReferenceNumber when SECURE_ELECTRONIC_TRANSFER service is included" in {
      val org = underTest.generateTestOrganisation(Seq(SECURE_ELECTRONIC_TRANSFER))

      org shouldHave(secureElectronicTransferReferenceNumberDefined = true)
    }

    "generate a pensionSchemeAdministratorIdentifier when RELIEF_AT_SOURCE service is included" in {
      val org = underTest.generateTestOrganisation(Seq(RELIEF_AT_SOURCE))

      org shouldHave(pensionSchemeAdministratorIdentifierDefined = true)
    }

    "generate an EORI when CUSTOMS_SERVICES service is included" in {
      val org = underTest.generateTestOrganisation(Seq(CUSTOMS_SERVICES))

      org shouldHave(eoriDefined = true)
    }

    "set the userFullName and emailAddress" in {
      val organisation = underTest.generateTestOrganisation(Seq(MTD_INCOME_TAX))

      organisation.userFullName.matches("[a-zA-Z]+ [a-zA-Z]+") shouldBe true

      val nameParts = organisation.userFullName.split(" ")

      organisation.emailAddress shouldBe s"${nameParts(0)}.${nameParts(1)}@example.com".toLowerCase
    }
  }

  "generateTestAgent" should {

    implicit def agentChecker(agent: TestAgent) = new Checker {
      def shouldHave(arnDefined: Boolean = false) = {

        check(agent.arn, arnDefined)
      }
    }

    "create a different test agent at every run" in {
      val agent1 = underTest.generateTestAgent(Seq(AGENT_SERVICES))
      val agent2 = underTest.generateTestAgent(Seq(AGENT_SERVICES))

      agent1 should haveDifferentPropertiesThan(agent2)
    }

    "not generate any identifiers when no services are included" in {
      val agent = underTest.generateTestAgent(Seq.empty)

      agent shouldHave(arnDefined = false)
    }

    "generate an agent reference number when AGENT_SERVICES service is included" in {
      val agent = underTest.generateTestAgent(Seq(AGENT_SERVICES))

      agent shouldHave(arnDefined = true)
    }

    "set the userFullName and emailAddress" in {
      val agent = underTest.generateTestAgent(Seq(AGENT_SERVICES))

      agent.userFullName.matches("[a-zA-Z]+ [a-zA-Z]+") shouldBe true

      val nameParts = agent.userFullName.split(" ")

      agent.emailAddress shouldBe s"${nameParts(0)}.${nameParts(1)}@example.com".toLowerCase
    }
  }

  "VrnChecksum" should {
    "generate valid VRN checksum" in {
      forAll(Gen.choose(6660000, 6669999)) { vrnBase =>
        val vrn  = VrnChecksum.apply(vrnBase.toString)
        VrnChecksum.isValid(vrn) shouldBe true
        println(vrn)
      }
    }

    "validate VRN" in {
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

object CustomMatchers {
  class HaveDifferentPropertiesThan(right: TestUser) extends Matcher[TestUser] {

    def apply(left: TestUser) = {
      MatchResult(
        allFieldsDifferent(left),
        s"""Individuals $left and $right have same fields"""",
        s"""Individuals are different""""
      )
    }

    private def allFieldsDifferent(leftUser: TestUser) = {
      (leftUser, right) match {
        case (i1: TestIndividual, i2: TestIndividual) => i1._id != i2._id  &&
          i1.userId != i2.userId &&
          i1.password != i2.password &&
          i1.nino != i2.nino &&
          i1.saUtr != i2.saUtr &&
          i1.eoriNumber != i2.eoriNumber

        case (o1: TestOrganisation, o2: TestOrganisation) => o1._id != o2._id &&
          o1.userId != o2.userId &&
          o1.password != o2.password &&
          o1.saUtr != o2.saUtr &&
          o1.ctUtr != o2.ctUtr &&
          o1.empRef != o2.empRef &&
          o1.vrn != o2.vrn &&
          o1.lisaManRefNum != o2.lisaManRefNum &&
          o1.secureElectronicTransferReferenceNumber != o2.secureElectronicTransferReferenceNumber &&
          o1.pensionSchemeAdministratorIdentifier != o2.pensionSchemeAdministratorIdentifier &&
          o1.eoriNumber != o2.eoriNumber

        case (a1: TestAgent, a2: TestAgent) => a1._id != a2._id &&
          a1.userId != a2.userId &&
          a1.password != a2.password &&
          a1.arn != a2.arn

        case _ => false
      }
    }
  }

  def haveDifferentPropertiesThan(right: TestUser) = new HaveDifferentPropertiesThan(right)
}
