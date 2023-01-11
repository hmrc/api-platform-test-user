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

package uk.gov.hmrc.testuser.models.identifiers

import uk.gov.hmrc.testuser.common.utils.HmrcSpec
import uk.gov.hmrc.testuser.models.Crn

class CrnSpec extends HmrcSpec {
  val crn = Crn("12345678")

  "valid Crn returns inner value with value" in {
    crn.value shouldBe "12345678"
  }

  "valid crn returns correct value for name" in {
    crn.name shouldBe "crn"
  }

  "throws Illegal argument exception when invalid creation tried" in {
    assertThrows[IllegalArgumentException] {
      val _ = Crn("{abc123}")
    }
  }
}
