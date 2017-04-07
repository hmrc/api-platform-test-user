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

package unit.uk.gov.hmrc.testuser.connectors

import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.connectors.{Enrolment, GovernmentGatewayLogin, TaxIdentifier}
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.ServiceName._

class GovernmentGatewayLoginSpec extends UnitSpec {
  val user = "user"
  val password = "password"

  val arn = AgentBusinessUtr("NARN0396245")
  val saUtr = SaUtr("1555369052")
  val nino = Nino("CC333333C")
  val mtdItId = MtdItId("XGIT00000000054")
  val ctUtr = CtUtr("1555369053")
  val vrn = Vrn("999902541")
  val empRef = EmpRef("555","EIA000")

  val agentEnrolment = Enrolment("HMRC-AS-AGENT", Seq(TaxIdentifier("AgentReferenceNumber", arn.utr)))
  val saEnrolment = Enrolment("IR-SA", Seq(TaxIdentifier("UTR", saUtr.toString)))
  val mtdItEnrolment = Enrolment("HMRC-MTD-IT", Seq(TaxIdentifier("MTDITID", mtdItId.toString)))
  val ctEnrolment = Enrolment("IR-CT", Seq(TaxIdentifier("UTR", ctUtr.toString)))
  val vatEnrolment = Enrolment("HMCE-VATDEC-ORG", Seq(TaxIdentifier("VATRegNo", vrn.toString)))
  val payeEnrolment = Enrolment("IR-PAYE", Seq(TaxIdentifier("TaxOfficeNumber", empRef.taxOfficeNumber),
    TaxIdentifier("TaxOfficeReference", empRef.taxOfficeReference)))

  "A GovernmentGatewayLogin created from a TestAgent" should {
    "contain no enrolments when the agent has no services" in {
      val testAgent = TestAgent(user, password, arn, Seq.empty)
      GovernmentGatewayLogin(testAgent).enrolments shouldBe empty
    }

    "contain the enrolment HMRC-AS-AGENT when the agent has the 'agent-services' service" in {
      val testAgent = TestAgent(user, password, arn, Seq(AGENT_SERVICES))
      val enrolments = GovernmentGatewayLogin(testAgent).enrolments
      GovernmentGatewayLogin(testAgent).enrolments should contain theSameElementsAs Seq(agentEnrolment)
    }

    "ignore services that are not applicable" in {
      val testAgent = TestAgent(user, password, arn, Seq(SELF_ASSESSMENT, AGENT_SERVICES))
      GovernmentGatewayLogin(testAgent).enrolments should contain theSameElementsAs Seq(agentEnrolment)
    }
  }

  "A GovernmentGatewayLogin created from a TestIndividual" should {
    "contain no enrolments when the individual has no services" in {
      val individual = TestIndividual(user, password, saUtr, nino, mtdItId)
      GovernmentGatewayLogin(individual).enrolments shouldBe empty
    }

    "contain the right enrolments for the individual's services" in {
      val individual = TestIndividual(user, password, saUtr, nino, mtdItId,
         Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX))

      GovernmentGatewayLogin(individual).enrolments should contain theSameElementsAs Seq(saEnrolment, mtdItEnrolment)
    }

    "ignore services that are not applicable" in {
      val individual = TestIndividual(user, password, saUtr, nino, mtdItId,
        Seq(AGENT_SERVICES, NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX))

      GovernmentGatewayLogin(individual).enrolments should contain theSameElementsAs Seq(saEnrolment, mtdItEnrolment)
    }
  }

  "A GovernmentGatewayLogin created from a TestOrganisation" should {
    "contain no enrolments when the organisation has no services" in {
      val organisation = TestOrganisation(user, password, saUtr, nino, mtdItId, empRef, ctUtr, vrn)
      GovernmentGatewayLogin(organisation).enrolments shouldBe empty
    }

    "contain the right enrolments for the organisation's services" in {
      val organisation = TestOrganisation(user, password, saUtr, nino, mtdItId, empRef, ctUtr, vrn,
        Seq(AGENT_SERVICES, NATIONAL_INSURANCE, SELF_ASSESSMENT, CORPORATION_TAX, SUBMIT_VAT_RETURNS, PAYE_FOR_EMPLOYERS, MTD_INCOME_TAX))

      GovernmentGatewayLogin(organisation).enrolments should contain theSameElementsAs
        Seq(saEnrolment, ctEnrolment, vatEnrolment, payeEnrolment, mtdItEnrolment)
    }

    "ignore services that are not applicable" in {
      val organisation = TestOrganisation(user, password, saUtr, nino, mtdItId, empRef, ctUtr, vrn,
        Seq(AGENT_SERVICES, NATIONAL_INSURANCE, CORPORATION_TAX, SUBMIT_VAT_RETURNS))

        GovernmentGatewayLogin(organisation).enrolments should contain theSameElementsAs Seq(ctEnrolment, vatEnrolment)
    }
  }

}
