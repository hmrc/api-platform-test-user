/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.testuser.util

import com.typesafe.config.ConfigFactory
import org.joda.time.LocalDate
import org.scalatest.{FlatSpec, Matchers}

class RandomiserSpec extends FlatSpec with Matchers {

  private val randomiser = new Randomiser {
    val config = ConfigFactory.parseString(
      """
        |randomiser {
        |
        |  oneEntry = [
        |      "entry"
        |    ]
        |
        |  twoEntries = [
        |    "entry1",
        |    "entry2"
        |  ]
        |
        |}
        |""".stripMargin
    )
  }

  "Randomiser" should "generate a random nino eligible date of birth" in {
    val date1 = randomiser.randomNinoEligibleDateOfBirth()
    val date2 = randomiser.randomNinoEligibleDateOfBirth()

    date1 should not be date2
    for (date <- Set(date1, date2)) {
      val now = LocalDate.now()

       date.isBefore(now.minusYears(16)) && date.isAfter(now.minusYears(101)) shouldBe true
    }
  }

  it should "retrieve a random string value form a random string configuration key" in {
    randomiser.randomConfigString("randomiser.oneEntry") shouldBe "entry"
    randomiser.randomConfigString("randomiser.twoEntries") should (equal("entry1") or equal("entry2"))
  }

}
