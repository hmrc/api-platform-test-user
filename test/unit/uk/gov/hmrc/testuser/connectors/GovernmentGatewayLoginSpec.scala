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
import uk.gov.hmrc.testuser.models.TestAgent


class GovernmentGatewayLoginSpec extends UnitSpec{
  val user = "user"
  val password = "password"

  val arn = AgentBusinessUtr("NARN0396245")

  "Create TestAgent with no services" should {
    "contain no enrolments" in {

      val testAgent = TestAgent(user, password, arn, None)
      GovernmentGatewayLogin(testAgent).enrolments shouldBe empty
    }
  }

  "Create TestAgent with empty service list" should {
    "contain no enrolments" in {

      val testAgent = TestAgent(user, password, arn, Some(Seq.empty))
      GovernmentGatewayLogin(testAgent).enrolments shouldBe empty

    }
  }
  "Create TestAgent with 'agent-services' service" should {
    "contain the enrolement HMRC-AS-AGENT only" in {

      val testAgent = TestAgent(user, password, arn, Some(Seq("agent-services")))
      val enrolments = GovernmentGatewayLogin(testAgent).enrolments
      enrolments.size shouldBe 1
      enrolments.head shouldBe Enrolment("HMRC-AS-AGENT", Seq(TaxIdentifier("AgentReferenceNumber", arn.utr)), "Activated")
    }
  }
}
