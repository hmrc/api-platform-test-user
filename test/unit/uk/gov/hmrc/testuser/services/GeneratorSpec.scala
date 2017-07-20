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

package unit.uk.gov.hmrc.testuser.services

import org.joda.time.LocalDate
import org.scalatest.matchers.{MatchResult, Matcher}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.ServiceName._
import uk.gov.hmrc.testuser.services.Generator
import unit.uk.gov.hmrc.testuser.services.CustomMatchers.haveDifferentPropertiesThan

class GeneratorSpec extends UnitSpec {

  val underTest = new Generator {
    override val fileName = "randomiser-unique-values"
  }

  "generateTestIndividual" should {

    "create a different test individual at every run" in {
      val individual1 = underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX))
      val individual2 = underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX))

      individual1 should haveDifferentPropertiesThan(individual2)
    }

    "generate a NINO and MTD IT ID when MTD_INCOME_TAX service is included" in {
      val individual = underTest.generateTestIndividual(Seq(MTD_INCOME_TAX))

      individual.mtdItId shouldBe defined
      individual.nino shouldBe defined
      individual.saUtr shouldBe empty
    }

    "generate a NINO when NATIONAL_INSURANCE service is included" in {
      val individual = underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE))

      individual.nino shouldBe defined
      individual.mtdItId shouldBe empty
      individual.saUtr shouldBe empty
    }

    "generate a SA UTR when SELF_ASSESSMENT service is included" in {
      val individual = underTest.generateTestIndividual(Seq(SELF_ASSESSMENT))

      individual.saUtr shouldBe defined
      individual.nino shouldBe empty
      individual.mtdItId shouldBe empty
    }

    "generate individualDetails from the configuration file" in {
      val individual = underTest.generateTestIndividual(Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX))

      individual.individualDetails shouldBe IndividualDetails("Adrian", "Adams", LocalDate.parse("1940-10-10"),
        Address("1 Abbey Road", "Aberdeen", "TS1 1PA"))
    }
  }

  "generateTestOrganisation" should {

    "create a different test organisation at every run" in {
      val organisation1 = underTest.generateTestOrganisation(Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX,
        CORPORATION_TAX, PAYE_FOR_EMPLOYERS, SUBMIT_VAT_RETURNS, LISA, SECURE_ELECTRONIC_TRANSFER))
      val organisation2 = underTest.generateTestOrganisation(Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX,
        CORPORATION_TAX, PAYE_FOR_EMPLOYERS, SUBMIT_VAT_RETURNS, LISA, SECURE_ELECTRONIC_TRANSFER))

      organisation1 should haveDifferentPropertiesThan(organisation2)
    }

    "generate a NINO and MTD IT ID when MTD_INCOME_TAX service is included" in {
      val org = underTest.generateTestOrganisation(Seq(MTD_INCOME_TAX))

      org.mtdItId shouldBe defined
      org.nino shouldBe defined
      org.empRef shouldBe empty
      org.ctUtr shouldBe empty
      org.saUtr shouldBe empty
      org.vrn shouldBe empty
      org.lisaManRefNum shouldBe empty
      org.secureElectronicTransferReferenceNumber shouldBe empty
    }

    "generate a NINO when NATIONAL_INSURANCE service is included" in {
      val org = underTest.generateTestOrganisation(Seq(NATIONAL_INSURANCE))

      org.nino shouldBe defined
      org.mtdItId shouldBe empty
      org.empRef shouldBe empty
      org.ctUtr shouldBe empty
      org.saUtr shouldBe empty
      org.vrn shouldBe empty
      org.lisaManRefNum shouldBe empty
      org.secureElectronicTransferReferenceNumber shouldBe empty
    }

    "generate a EMPREF when PAYE_FOR_EMPLOYERS service is included" in {
      val org = underTest.generateTestOrganisation(Seq(PAYE_FOR_EMPLOYERS))

      org.empRef shouldBe defined
      org.nino shouldBe empty
      org.mtdItId shouldBe empty
      org.ctUtr shouldBe empty
      org.saUtr shouldBe empty
      org.vrn shouldBe empty
      org.lisaManRefNum shouldBe empty
      org.secureElectronicTransferReferenceNumber shouldBe empty
    }

    "generate a CT UTR when CORPORATION_TAX service is included" in {
      val org = underTest.generateTestOrganisation(Seq(CORPORATION_TAX))

      org.ctUtr shouldBe defined
      org.nino shouldBe empty
      org.mtdItId shouldBe empty
      org.empRef shouldBe empty
      org.saUtr shouldBe empty
      org.vrn shouldBe empty
      org.lisaManRefNum shouldBe empty
      org.secureElectronicTransferReferenceNumber shouldBe empty
    }

    "generate a SA UTR when SELF_ASSESSMENT service is included" in {
      val org = underTest.generateTestOrganisation(Seq(SELF_ASSESSMENT))

      org.saUtr shouldBe defined
      org.nino shouldBe empty
      org.mtdItId shouldBe empty
      org.empRef shouldBe empty
      org.ctUtr shouldBe empty
      org.vrn shouldBe empty
      org.lisaManRefNum shouldBe empty
      org.secureElectronicTransferReferenceNumber shouldBe empty
    }

    "generate a VRN when SUBMIT_VAT_RETURNS service is included" in {
      val org = underTest.generateTestOrganisation(Seq(SUBMIT_VAT_RETURNS))

      org.vrn shouldBe defined
      org.nino shouldBe empty
      org.mtdItId shouldBe empty
      org.empRef shouldBe empty
      org.ctUtr shouldBe empty
      org.saUtr shouldBe empty
      org.lisaManRefNum shouldBe empty
      org.secureElectronicTransferReferenceNumber shouldBe empty
    }

    "generate a lisaManagerReferenceNumber when LISA service is included" in {
      val org = underTest.generateTestOrganisation(Seq(LISA))

      org.vrn shouldBe empty
      org.nino shouldBe empty
      org.mtdItId shouldBe empty
      org.empRef shouldBe empty
      org.ctUtr shouldBe empty
      org.saUtr shouldBe empty
      org.lisaManRefNum shouldBe defined
      org.secureElectronicTransferReferenceNumber shouldBe empty
    }

    "generate a secureElectronicTransferReferenceNumber when SECURE_ELECTRONIC_TRANSFER service is included" in {
      val org = underTest.generateTestOrganisation(Seq(SECURE_ELECTRONIC_TRANSFER))

      org.vrn shouldBe empty
      org.nino shouldBe empty
      org.mtdItId shouldBe empty
      org.empRef shouldBe empty
      org.ctUtr shouldBe empty
      org.saUtr shouldBe empty
      org.lisaManRefNum shouldBe empty
      org.secureElectronicTransferReferenceNumber shouldBe defined
    }

  }

  "generateTestAgent" should {

    "create a different test agent at every run" in {
      val agent1 = underTest.generateTestAgent(Seq(AGENT_SERVICES))
      val agent2 = underTest.generateTestAgent(Seq(AGENT_SERVICES))

      agent1 should haveDifferentPropertiesThan(agent2)
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
          i1.saUtr != i2.saUtr

        case (o1: TestOrganisation, o2: TestOrganisation) => o1._id != o2._id &&
          o1.userId != o2.userId &&
          o1.password != o2.password &&
          o1.saUtr != o2.saUtr &&
          o1.ctUtr != o2.ctUtr &&
          o1.empRef != o2.empRef &&
          o1.vrn != o2.vrn &&
          o1.lisaManRefNum != o2.lisaManRefNum &&
          o1.secureElectronicTransferReferenceNumber != o2.secureElectronicTransferReferenceNumber

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
