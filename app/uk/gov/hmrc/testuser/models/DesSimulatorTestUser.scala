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

case class DesSimulatorTestIndividual(mtdItId: Option[String], vrn: Option[String], nino: Option[String], saUtr: Option[String])

object DesSimulatorTestIndividual {
  def from(individual: TestIndividual) = DesSimulatorTestIndividual(individual.mtdItId, individual.vrn, individual.nino, individual.saUtr)
}

case class DesSimulatorTestOrganisation(
    mtdItId: Option[String],
    nino: Option[String],
    saUtr: Option[String],
    ctUtr: Option[String],
    empRef: Option[String],
    vrn: Option[String]
  )

object DesSimulatorTestOrganisation {

  def from(organisation: TestOrganisation) =
    DesSimulatorTestOrganisation(
      organisation.mtdItId,
      organisation.nino,
      organisation.saUtr,
      organisation.ctUtr,
      organisation.empRef,
      organisation.vrn
    )
}
