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

import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting

sealed trait UserType

object UserType {
  case object INDIVIDUAL   extends UserType
  case object ORGANISATION extends UserType
  case object AGENT        extends UserType

  val values: Set[UserType] = Set(INDIVIDUAL, ORGANISATION, AGENT)

  def apply(text: String): Option[UserType] = UserType.values.find(_.toString == text.toUpperCase)

  def unsafeApply(text: String): UserType = {
    apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid User Type"))
  }

  import play.api.libs.json.Format

  implicit val format: Format[UserType] = SealedTraitJsonFormatting.createFormatFor[UserType]("User Type", apply(_))
}
