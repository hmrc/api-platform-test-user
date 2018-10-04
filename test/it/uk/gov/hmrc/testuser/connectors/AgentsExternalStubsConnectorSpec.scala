/*
 * Copyright 2018 HM Revenue & Customs
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

package it.uk.gov.hmrc.testuser.connectors

import it.uk.gov.hmrc.testuser.helpers.stubs.AgentsExternalStubsStub
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.testuser.config.AppContext
import uk.gov.hmrc.testuser.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.testuser.models.ServiceName._
import uk.gov.hmrc.testuser.services.Generator

class AgentsExternalStubsConnectorSpec extends UnitSpec with BeforeAndAfterEach with WithFakeApplication {

  val generator = new Generator()
  val testIndividual = generator.generateTestIndividual(Seq(MTD_INCOME_TAX, SELF_ASSESSMENT, NATIONAL_INSURANCE))
  val testOrganisation = generator.generateTestOrganisation(Seq(MTD_INCOME_TAX, SELF_ASSESSMENT, NATIONAL_INSURANCE, CORPORATION_TAX))

  trait Setup {
    implicit val hc = HeaderCarrier()

    val appContext = new AppContext {
      override def passwordLogRounds: Int = 0
      override def syncToAgentsExternalStubs: Boolean = true
    }

    val underTest = new AgentsExternalStubsConnector(appContext) {
      override lazy val serviceUrl: String = AgentsExternalStubsStub.url
    }
  }

  override def beforeAll() = {
    super.beforeAll()
    AgentsExternalStubsStub.server.start()
  }

  override def beforeEach() = {
    super.beforeEach()
    AgentsExternalStubsStub.server.resetMappings()
  }

  override def afterAll() = {
    super.afterAll()
    AgentsExternalStubsStub.server.stop()
  }

  "createTestUser" should {
    "create a test individual" in new Setup {
      AgentsExternalStubsStub.willSuccessfullyCreateTestUser()

      await(underTest.createTestUser(testIndividual))
    }

    "create a test organisation" in new Setup {
      AgentsExternalStubsStub.willSuccessfullyCreateTestUser()

      await(underTest.createTestUser(testOrganisation))
    }

    "fail when the DesSimulator returns an error" in new Setup {
      AgentsExternalStubsStub.willFailWhenCreatingTestUser()
      intercept[Upstream5xxResponse]{await(underTest.createTestUser(testIndividual))}
    }
  }
}
