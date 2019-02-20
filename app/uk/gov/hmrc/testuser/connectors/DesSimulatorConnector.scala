/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.testuser.connectors

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesSimulatorConnector @Inject()(httpClient: HttpClient, override val runModeConfiguration: Configuration, environment: Environment)
                           (implicit ec: ExecutionContext) extends ServicesConfig {

  override protected def mode = environment.mode

  lazy val serviceUrl: String = baseUrl("des-simulator")

  def createIndividual(individual: TestIndividual)(implicit hc: HeaderCarrier): Future[TestIndividual] = {
    Logger.info(s"Calling des-simulator ($serviceUrl) to create individual $individual")
    httpClient.POST(s"$serviceUrl/test-users/individuals", DesSimulatorTestIndividual.from(individual)) map { _ => individual }
  }

  def createOrganisation(organisation: TestOrganisation)(implicit hc: HeaderCarrier): Future[TestOrganisation] = {
    Logger.info(s"Calling des-simulator ($serviceUrl) to create organisation $organisation")
    httpClient.POST(s"$serviceUrl/test-users/organisations", DesSimulatorTestOrganisation.from(organisation)) map { _ => organisation }
  }
}
