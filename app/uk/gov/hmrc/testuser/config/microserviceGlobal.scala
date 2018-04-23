/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.testuser.config

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Play._
import play.api._
import uk.gov.hmrc.api.config.{ServiceLocatorConfig, ServiceLocatorRegistration}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal
import uk.gov.hmrc.play.microservice.filters.{AuditFilter, LoggingFilter, MicroserviceFilterSupport}
import uk.gov.hmrc.api.connector.ServiceLocatorConnector

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object AuthParamsControllerConfiguration extends AuthParamsControllerConfig {
  lazy val controllerConfigs = ControllerConfiguration.controllerConfigs
}

object MicroserviceAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport {
  override val auditConnector = MicroserviceAuditConnector

  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object MicroserviceLoggingFilter extends LoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceAuthFilter extends AuthorisationFilter with MicroserviceFilterSupport {
  override lazy val authParamsConfig = AuthParamsControllerConfiguration
  override lazy val authConnector = MicroserviceAuthConnector

  override def controllerNeedsAuth(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuth
}

object MicroserviceGlobal extends DefaultMicroserviceGlobal with ServiceLocatorRegistration with ServiceLocatorConfig
  with RunMode with MicroserviceFilterSupport {

  override val hc = HeaderCarrier()
  override val slConnector = ServiceLocatorConnector(WSHttp)
  override val auditConnector = MicroserviceAuditConnector
  override lazy val registrationEnabled = current.configuration.getBoolean("microservice.services.service-locator.enabled").getOrElse(false)

  override def microserviceMetricsConfig(implicit app: Application) = app.configuration.getConfig("microservice.metrics")

  override val loggingFilter = MicroserviceLoggingFilter
  override val microserviceAuditFilter = MicroserviceAuditFilter
  override val authFilter = Some(MicroserviceAuthFilter)
}
