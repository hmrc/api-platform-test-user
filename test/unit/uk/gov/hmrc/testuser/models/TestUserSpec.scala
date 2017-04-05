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

package unit.uk.gov.hmrc.testuser.models

import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.models.MtdId

class TestUserSpec extends UnitSpec {

  "MTD ID should accept a valid ID" in {

    val mtdId = MtdId("XGIT00000000054")

    mtdId.toString shouldBe "XGIT00000000054"
  }

  "MTD ID should not accept an ID with invalid checksum" in {
    intercept[IllegalArgumentException] {
      MtdId("XXIT00000000054")
    }
  }
}
