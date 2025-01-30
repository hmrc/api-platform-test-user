/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalatest.prop.TableDrivenPropertyChecks

import play.api.libs.json._
import uk.gov.hmrc.apiplatform.modules.common.utils.BaseJsonFormattersSpec

class ServiceKeySpec extends BaseJsonFormattersSpec with TableDrivenPropertyChecks {

  val values =
    Table(
      ("Source", "text"),
      (ServiceKey.NATIONAL_INSURANCE, "national-insurance"),
      (ServiceKey.SELF_ASSESSMENT, "self-assessment"),
      (ServiceKey.CORPORATION_TAX, "corporation-tax"),
      (ServiceKey.PAYE_FOR_EMPLOYERS, "paye-for-employers"),
      (ServiceKey.SUBMIT_VAT_RETURNS, "submit-vat-returns"),
      (ServiceKey.MTD_VAT, "mtd-vat"),
      (ServiceKey.MTD_INCOME_TAX, "mtd-income-tax"),
      (ServiceKey.AGENT_SERVICES, "agent-services"),
      (ServiceKey.LISA, "lisa"),
      (ServiceKey.SECURE_ELECTRONIC_TRANSFER, "secure-electronic-transfer"),
      (ServiceKey.RELIEF_AT_SOURCE, "relief-at-source"),
      (ServiceKey.CUSTOMS_SERVICES, "customs-services"),
      (ServiceKey.GOODS_VEHICLE_MOVEMENTS, "goods-vehicle-movements"),
      (ServiceKey.IMPORT_CONTROL_SYSTEM, "import-control-system"),
      (ServiceKey.SAFETY_AND_SECURITY, "safety-and-security"),
      (ServiceKey.CTC, "common-transit-convention-traders"),
      (ServiceKey.CTC_LEGACY, "common-transit-convention-traders-legacy"),
      (ServiceKey.EMCS, "excise-movement-control-system"),
      (ServiceKey.MODERNISATION_OF_AUTHORISATIONS, "modernisation-of-authorisations"),
      (ServiceKey.PILLAR_2, "pillar-2")
    )

  "ServiceKeys" when {
    "convert to string correctly" in {
      forAll(values) { (s, t) =>
        s.toString() shouldBe t
      }
    }

    "convert string to case object" in {
      forAll(values) { (s, t) =>
        ServiceKey.apply(t) shouldBe Some(s)
        ServiceKey.unsafeApply(t) shouldBe s
      }
    }

    "convert string value to None when undefined or empty" in {
      ServiceKey.apply("rubbish") shouldBe None
      ServiceKey.apply("") shouldBe None
    }

    "throw when string value is invalid" in {
      intercept[RuntimeException] {
        ServiceKey.unsafeApply("rubbish")
      }.getMessage() should include("Service Key")
    }

    "read from Json" in {
      forAll(values) { (s, t) =>
        testFromJson[ServiceKey](s""""$t"""")(s)
      }
    }

    "read with text error from Json" in {
      intercept[Exception] {
        testFromJson[ServiceKey](s""" "123" """)(ServiceKey.AGENT_SERVICES)
      }.getMessage() should include("123 is not a valid Service Key")
    }

    "read with error from Json" in {
      intercept[Exception] {
        testFromJson[ServiceKey](s"""123""")(ServiceKey.AGENT_SERVICES)
      }.getMessage() should include("Cannot parse Service Key from '123'")
    }

    "write to Json" in {
      forAll(values) { (s, t) =>
        Json.toJson[ServiceKey](s) shouldBe JsString(t)
      }
    }
  }
}
