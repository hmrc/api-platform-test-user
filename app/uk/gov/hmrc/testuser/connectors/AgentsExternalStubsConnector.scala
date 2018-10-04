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

package uk.gov.hmrc.testuser.connectors

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.testuser.config.{AppContext, WSHttp}
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AgentsExternalStubsConnector @Inject()(appContext: AppContext) extends ServicesConfig {
  lazy val serviceUrl: String = baseUrl("agents-external-stubs")

  def createTestUser(user: TestUser)(implicit hc:HeaderCarrier): Future[Unit] = {
    if(appContext.syncToAgentsExternalStubs) {
      Logger.info(s"Calling agents-external-stubs ($serviceUrl) to create user ${user.userId}")
      WSHttp.POST(s"$serviceUrl/agents-external-stubs/users/api-platform", user) map { _ => () }
    } else Future.successful(())
  }
}
