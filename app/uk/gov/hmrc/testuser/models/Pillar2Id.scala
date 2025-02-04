/*
 * Copyright 2025 HM Revenue & Customs
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

sealed trait AllowedDuplicatePillar2Ids {
  def value: String
}

object AllowedDuplicatePillar2Ids {
  case object BAD_REQUEST_ID           extends AllowedDuplicatePillar2Ids { val value = "XEPLR4000000000" }
  case object INTERNAL_SERVER_ERROR_ID extends AllowedDuplicatePillar2Ids { val value = "XEPLR5000000000" }
  case object UNPROCESSABLE_ENTITY_ID  extends AllowedDuplicatePillar2Ids { val value = "XEPLR4220000000" }

  val values: Set[String] = Set(
    BAD_REQUEST_ID.value,
    INTERNAL_SERVER_ERROR_ID.value,
    UNPROCESSABLE_ENTITY_ID.value
  )
}
