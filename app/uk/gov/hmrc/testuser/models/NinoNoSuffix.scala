/*
 * Copyright 2020 HM Revenue & Customs
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

import uk.gov.hmrc.domain._

case class NinoNoSuffix(value: String) {
  require(NinoNoSuffix.isValid(value), s"$value is not a valid nino.")

  override def toString = value
}

object NinoNoSuffix {
  def isValid(nino: String) = nino != null && Nino.isValid(nino + "A")
  def apply(nino: Nino): NinoNoSuffix = NinoNoSuffix(nino.nino.substring(0, nino.nino.length - 1))
}
