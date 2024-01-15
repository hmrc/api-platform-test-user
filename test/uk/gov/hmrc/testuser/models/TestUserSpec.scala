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

package uk.gov.hmrc.testuser.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TestUserSpec extends AnyFlatSpec with Matchers {
  val userId          = "1234567890"
  val groupIdentifier = "groupIdentifier"
  val lisaRefNum      = "Z123456"
  val password        = "l3tm31n"
  val userFullName    = "John Doe"
  val emailAddress    = "john.doe@example.com"
  val arn             = "NARN0396245"
  val agentCode       = "1234509876"

  "MTD ID" should "accept a valid ID" in {
    val mtdItId = MtdItId("XGIT00000000054")

    mtdItId.toString shouldBe "XGIT00000000054"
  }

  "MTD ID" should "not accept an ID with invalid checksum" in {
    intercept[IllegalArgumentException] {
      MtdItId("XXIT00000000054")
    }
  }

  "TestOrganisationCreatedResponse" should "be properly constructed from a TestOrganisation" in {
    val organisationDetails = OrganisationDetails("Company ABCDEF", Address("225 Baker St", "Marylebone", "NW1 6XE"))
    val testOrganisation    = TestOrganisation(
      userId = userId,
      password = password,
      userFullName = userFullName,
      emailAddress = emailAddress,
      organisationDetails = organisationDetails,
      individualDetails = None,
      props = Map(
        TestUserPropKey.lisaManRefNum   -> lisaRefNum,
        TestUserPropKey.groupIdentifier -> groupIdentifier
      )
    )

    TestOrganisationCreatedResponse.from(testOrganisation) shouldBe
      TestOrganisationCreatedResponse(
        userId = userId,
        password = password,
        userFullName = userFullName,
        emailAddress = emailAddress,
        organisationDetails = organisationDetails,
        individualDetails = None,
        props = Map(
          "lisaManagerReferenceNumber" -> lisaRefNum,
          "groupIdentifier"            -> groupIdentifier
        )
      )
  }

  "TestAgentCreatedResponse" should "be properly constructed from the TestAgent" in {
    val testAgent = TestAgent(
      userId = userId,
      password = password,
      userFullName = userFullName,
      emailAddress = emailAddress,
      props = Map(
        TestUserPropKey.arn             -> arn,
        TestUserPropKey.agentCode       -> "1234509876",
        TestUserPropKey.groupIdentifier -> groupIdentifier
      )
    )

    TestAgentCreatedResponse.from(testAgent) shouldBe TestAgentCreatedResponse(
      userId,
      password,
      userFullName,
      emailAddress,
      Map(
        "agentServicesAccountNumber" -> arn,
        "agentCode"                  -> agentCode,
        "groupIdentifier"            -> groupIdentifier
      )
    )
  }

  "Services" should "get size equal to all services when length called" in {
    Services.all.length shouldBe 18
  }
}
