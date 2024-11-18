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

import play.api.libs.json.{Format, Json}

import uk.gov.hmrc.testuser.models.UserType

case class Service(key: ServiceKey, name: String, allowedUserTypes: Seq[UserType])

object Service {
  implicit val formatServices: Format[Service] = Json.format[Service]
}

object Services {
  import UserType._

  val all = Seq(
    Service(ServiceKey.NATIONAL_INSURANCE, "National Insurance", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKey.SELF_ASSESSMENT, "Self Assessment", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKey.CORPORATION_TAX, "Corporation Tax", Seq(ORGANISATION)),
    Service(ServiceKey.PAYE_FOR_EMPLOYERS, "PAYE for Employers", Seq(ORGANISATION)),
    Service(ServiceKey.SUBMIT_VAT_RETURNS, "Submit VAT Returns", Seq(ORGANISATION)),
    Service(ServiceKey.MTD_VAT, "MTD VAT", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKey.MTD_INCOME_TAX, "MTD Income Tax", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKey.AGENT_SERVICES, "Agent Services", Seq(AGENT)),
    Service(ServiceKey.LISA, "Lifetime ISA", Seq(ORGANISATION)),
    Service(ServiceKey.SECURE_ELECTRONIC_TRANSFER, "Secure Electronic Transfer", Seq(ORGANISATION)),
    Service(ServiceKey.RELIEF_AT_SOURCE, "Relief at Source", Seq(ORGANISATION)),
    Service(ServiceKey.CUSTOMS_SERVICES, "Customs Services", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKey.GOODS_VEHICLE_MOVEMENTS, "Goods Vehicle Services", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKey.IMPORT_CONTROL_SYSTEM, "Import Control System", Seq(ORGANISATION)),
    Service(ServiceKey.CTC_LEGACY, "Common Transit Convention Traders Legacy", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKey.CTC, "Common Transit Convention Traders", Seq(INDIVIDUAL, ORGANISATION)),
    Service(ServiceKey.SAFETY_AND_SECURITY, "Safety and Security", Seq(ORGANISATION)),
    Service(ServiceKey.EMCS, "Excise Movement Control System", Seq(ORGANISATION)),
    Service(ServiceKey.MA, "Modernisation of Authorisations", Seq(ORGANISATION))
  )
}
