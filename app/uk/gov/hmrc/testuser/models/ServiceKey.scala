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

sealed trait ServiceKey {
  override def toString(): String = ServiceKey.asText(this)
}

object ServiceKey {
  case object NATIONAL_INSURANCE              extends ServiceKey
  case object SELF_ASSESSMENT                 extends ServiceKey
  case object CORPORATION_TAX                 extends ServiceKey
  case object PAYE_FOR_EMPLOYERS              extends ServiceKey
  case object SUBMIT_VAT_RETURNS              extends ServiceKey
  case object MTD_VAT                         extends ServiceKey
  case object MTD_INCOME_TAX                  extends ServiceKey
  case object AGENT_SERVICES                  extends ServiceKey
  case object LISA                            extends ServiceKey
  case object SECURE_ELECTRONIC_TRANSFER      extends ServiceKey
  case object RELIEF_AT_SOURCE                extends ServiceKey
  case object CUSTOMS_SERVICES                extends ServiceKey
  case object GOODS_VEHICLE_MOVEMENTS         extends ServiceKey
  case object IMPORT_CONTROL_SYSTEM           extends ServiceKey
  case object SAFETY_AND_SECURITY             extends ServiceKey
  case object CTC                             extends ServiceKey
  case object CTC_LEGACY                      extends ServiceKey
  case object EMCS                            extends ServiceKey
  case object TAX_FREE_CHILDCARE_PAYMENTS     extends ServiceKey
  case object MODERNISATION_OF_AUTHORISATIONS extends ServiceKey
  case object PILLAR_2                        extends ServiceKey

  private def asText(serviceKey: ServiceKey): String = serviceKey match {
    case NATIONAL_INSURANCE              => "national-insurance"
    case SELF_ASSESSMENT                 => "self-assessment"
    case CORPORATION_TAX                 => "corporation-tax"
    case PAYE_FOR_EMPLOYERS              => "paye-for-employers"
    case SUBMIT_VAT_RETURNS              => "submit-vat-returns"
    case MTD_VAT                         => "mtd-vat"
    case MTD_INCOME_TAX                  => "mtd-income-tax"
    case AGENT_SERVICES                  => "agent-services"
    case LISA                            => "lisa"
    case SECURE_ELECTRONIC_TRANSFER      => "secure-electronic-transfer"
    case RELIEF_AT_SOURCE                => "relief-at-source"
    case CUSTOMS_SERVICES                => "customs-services"
    case GOODS_VEHICLE_MOVEMENTS         => "goods-vehicle-movements"
    case IMPORT_CONTROL_SYSTEM           => "import-control-system"
    case SAFETY_AND_SECURITY             => "safety-and-security"
    case CTC                             => "common-transit-convention-traders"
    case CTC_LEGACY                      => "common-transit-convention-traders-legacy"
    case EMCS                            => "excise-movement-control-system"
    case TAX_FREE_CHILDCARE_PAYMENTS     => "tax-free-childcare-payments"
    case MODERNISATION_OF_AUTHORISATIONS => "modernisation-of-authorisations"
    case PILLAR_2                        => "pillar-2"
  }

  val values: Set[ServiceKey] = Set(
    NATIONAL_INSURANCE,
    SELF_ASSESSMENT,
    CORPORATION_TAX,
    PAYE_FOR_EMPLOYERS,
    SUBMIT_VAT_RETURNS,
    MTD_VAT,
    MTD_INCOME_TAX,
    AGENT_SERVICES,
    LISA,
    SECURE_ELECTRONIC_TRANSFER,
    RELIEF_AT_SOURCE,
    CUSTOMS_SERVICES,
    GOODS_VEHICLE_MOVEMENTS,
    IMPORT_CONTROL_SYSTEM,
    SAFETY_AND_SECURITY,
    CTC,
    CTC_LEGACY,
    EMCS,
    TAX_FREE_CHILDCARE_PAYMENTS,
    MODERNISATION_OF_AUTHORISATIONS,
    PILLAR_2
  )

  def apply(text: String): Option[ServiceKey] = ServiceKey.values.find(_.toString == text)

  def unsafeApply(text: String): ServiceKey = {
    apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Service Key"))
  }

  import play.api.libs.json.Format

  implicit val format: Format[ServiceKey] = SealedTraitJsonFormatting.createFormatFor[ServiceKey]("Service Key", apply(_))
}
