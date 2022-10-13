/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.testuser

import uk.gov.hmrc.domain.{CtUtr, EmpRef, Nino, SaUtr, Vrn}
import uk.gov.hmrc.testuser.models.{Crn, NinoNoSuffix}
import play.api.mvc.PathBindable

class SimpleObjectBinder[T](bind: String => T, unbind: T => String)(implicit m: Manifest[T]) extends PathBindable[T] {

  override def bind(key: String, value: String): Either[String, T] =
    try Right(bind(value))
    catch {
      case e: Throwable =>
        Left(s"Cannot parse parameter '$key' with value '$value' as '${m.runtimeClass.getSimpleName}'")
    }

  def unbind(key: String, value: T): String = unbind(value)
}

object NinoNoSuffixBinder extends SimpleObjectBinder[NinoNoSuffix](NinoNoSuffix.apply, _.value)
object NinoBinder         extends SimpleObjectBinder[Nino](Nino.apply, _.value)
object SaUtrBinder        extends SimpleObjectBinder[SaUtr](SaUtr.apply, _.value)
object EmpRefBinder       extends SimpleObjectBinder[EmpRef](EmpRef.fromIdentifiers, _.value)
object VrnBinder          extends SimpleObjectBinder[Vrn](Vrn.apply, _.value)
object CtUtrBinder        extends SimpleObjectBinder[CtUtr](CtUtr.apply, _.value)
object CrnBinder          extends SimpleObjectBinder[Crn](Crn.apply, _.value)

package object Binders {
  implicit val ninoNoSuffixBinder = NinoNoSuffixBinder
  implicit val ninoBinder         = NinoBinder
  implicit val saUtrBinder        = SaUtrBinder
  implicit val empRefBinder       = EmpRefBinder
  implicit val vrnBinder          = VrnBinder
  implicit val ctUtrBinder        = CtUtrBinder
  implicit val crnBinder          = CrnBinder
}
