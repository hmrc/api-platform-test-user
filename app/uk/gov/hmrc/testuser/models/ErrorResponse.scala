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

import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting

sealed trait ErrorCode

object ErrorCode {
  case object INTERNAL_SERVER_ERROR   extends ErrorCode
  case object INVALID_CREDENTIALS     extends ErrorCode
  case object USER_NOT_FOUND          extends ErrorCode
  case object NINO_ALREADY_USED       extends ErrorCode
  case object PILLAR2_ID_ALREADY_USED extends ErrorCode

  val values: Set[ErrorCode] = Set(INTERNAL_SERVER_ERROR, INVALID_CREDENTIALS, USER_NOT_FOUND, NINO_ALREADY_USED,PILLAR2_ID_ALREADY_USED)

  def apply(text: String): Option[ErrorCode] = ErrorCode.values.find(_.toString == text.toUpperCase)

  def unsafeApply(text: String): ErrorCode = {
    apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Error Code"))
  }

  import play.api.libs.json.Format

  implicit val format: Format[ErrorCode] = SealedTraitJsonFormatting.createFormatFor[ErrorCode]("Error Code", apply(_))
}

case class ErrorResponse(code: ErrorCode, message: String)

object ErrorResponse {
  val internalServerError       = ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
  val invalidCredentialsError   = ErrorResponse(ErrorCode.INVALID_CREDENTIALS, "Invalid Authentication information provided")
  val individualNotFoundError   = ErrorResponse(ErrorCode.USER_NOT_FOUND, "The individual can not be found")
  val organisationNotFoundError = ErrorResponse(ErrorCode.USER_NOT_FOUND, "The organisation can not be found")
  val ninoAlreadyUsed           = ErrorResponse(ErrorCode.NINO_ALREADY_USED, "The nino specified has already been used")
  val pillar2IdAlreadyUsed      = ErrorResponse(ErrorCode.PILLAR2_ID_ALREADY_USED, "The pillar2Id specified has already been used")
}
