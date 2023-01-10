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

package uk.gov.hmrc.testuser.helpers.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.HeaderNames.{AUTHORIZATION, LOCATION}
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.testuser.helpers.MockHost
import uk.gov.hmrc.testuser.models.AuthSession

object AuthLoginApiStub extends MockHost(11111) {

  def willReturnTheSession(session: AuthSession) = {
    mock.register(post(urlPathEqualTo("/government-gateway/session/login"))
      .willReturn(aResponse()
        .withStatus(CREATED)
        .withBody(s"""{"gatewayToken": "${session.gatewayToken}"}""")
        .withHeader(AUTHORIZATION, session.authBearerToken)
        .withHeader(LOCATION, session.authorityUri)))
  }

  def willFailToReturnASession() = {
    mock.register(post(urlPathEqualTo("/government-gateway/session/login"))
      .willReturn(aResponse()
        .withStatus(INTERNAL_SERVER_ERROR)))
  }
}
