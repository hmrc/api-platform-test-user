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

class UserTypeSpec extends BaseJsonFormattersSpec with TableDrivenPropertyChecks {

  val values =
    Table(
      ("Source", "text"),
      (UserType.INDIVIDUAL, "individual"),
      (UserType.ORGANISATION, "organisation"),
      (UserType.AGENT, "agent")
    )

  "UserTypes" when {
    "convert to string correctly" in {
      forAll(values) { (s, t) =>
        s.toString() shouldBe t.toUpperCase()
      }
    }

    "convert lower case string to case object" in {
      forAll(values) { (s, t) =>
        UserType.apply(t) shouldBe Some(s)
        UserType.unsafeApply(t) shouldBe s
      }
    }

    "convert mixed case string to case object" in {
      forAll(values) { (s, t) =>
        UserType.apply(t.toUpperCase()) shouldBe Some(s)
        UserType.unsafeApply(t.toUpperCase()) shouldBe s
      }
    }

    "convert string value to None when undefined or empty" in {
      UserType.apply("rubbish") shouldBe None
      UserType.apply("") shouldBe None
    }

    "throw when string value is invalid" in {
      intercept[RuntimeException] {
        UserType.unsafeApply("rubbish")
      }.getMessage() should include("User Type")
    }

    "read from Json" in {
      forAll(values) { (s, t) =>
        testFromJson[UserType](s""""$t"""")(s)
      }
    }

    "read with text error from Json" in {
      intercept[Exception] {
        testFromJson[UserType](s""" "123" """)(UserType.AGENT)
      }.getMessage() should include("123 is not a valid User Type")
    }

    "read with error from Json" in {
      intercept[Exception] {
        testFromJson[UserType](s"""123""")(UserType.AGENT)
      }.getMessage() should include("Cannot parse User Type from '123'")
    }

    "write to Json" in {
      forAll(values) { (s, t) =>
        Json.toJson[UserType](s) shouldBe JsString(t.toUpperCase())
      }
    }
  }
}
