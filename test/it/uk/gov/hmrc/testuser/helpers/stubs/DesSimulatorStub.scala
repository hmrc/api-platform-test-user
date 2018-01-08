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

package it.uk.gov.hmrc.testuser.helpers.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import it.uk.gov.hmrc.testuser.helpers.MockHost
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR}

object DesSimulatorStub extends MockHost(11112) {

  def willSuccessfullyCreateTestIndividual() = {
    mock.register(post(urlPathEqualTo("/test-users/individuals"))
        .willReturn(aResponse().withStatus(CREATED)))
  }

  def willFailWhenCreatingTestIndividual() = {
    mock.register(post(urlPathEqualTo("/test-users/individuals"))
        .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))
  }

  def willSuccessfullyCreateTestOrganisation() = {
    mock.register(post(urlPathEqualTo("/test-users/organisations"))
        .willReturn(aResponse().withStatus(CREATED)))
  }

  def willFailWhenCreatingTestOrganisation() = {
    mock.register(post(urlPathEqualTo("/test-users/organisations"))
      .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))
  }
}
