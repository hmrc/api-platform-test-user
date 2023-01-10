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

package uk.gov.hmrc.testuser.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.services.ApplicationLogger

@Singleton
class DesSimulatorConnector @Inject() (httpClient: HttpClient, runModeConfiguration: Configuration, environment: Environment, config: ServicesConfig)(implicit ec: ExecutionContext)
    extends ApplicationLogger {

  import config.baseUrl

  lazy val serviceUrl: String = baseUrl("des-simulator")

  def createIndividual(individual: TestIndividual)(implicit hc: HeaderCarrier): Future[TestIndividual] = {
    logger.info(s"Calling des-simulator ($serviceUrl) to create individual $individual")
    httpClient.POST[DesSimulatorTestIndividual, Either[UpstreamErrorResponse, HttpResponse]](s"$serviceUrl/test-users/individuals", DesSimulatorTestIndividual.from(individual)) map {
      case Right(_)  => individual
      case Left(err) => throw err
    }
  }

  def createOrganisation(organisation: TestOrganisation)(implicit hc: HeaderCarrier): Future[TestOrganisation] = {
    logger.info(s"Calling des-simulator ($serviceUrl) to create organisation $organisation")
    httpClient.POST[DesSimulatorTestOrganisation, Either[UpstreamErrorResponse, HttpResponse]](
      s"$serviceUrl/test-users/organisations",
      DesSimulatorTestOrganisation.from(organisation)
    ) map {
      case Right(_)  => organisation
      case Left(err) => throw err
    }
  }
}
