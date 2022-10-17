/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.http.Status.OK
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.testuser.controllers.DocumentationController

import uk.gov.hmrc.testuser.common.utils.AsyncHmrcSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

/**
  * Testcase to verify the capability of integration with the API platform.
  * 1a, To expose API's to Third Party Developers, the service needs to make the API definition available under api/definition GET endpoint
  * 1b, The endpoints need to be defined in an application.raml file for all versions
  */
class PlatformIntegrationSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite {

  trait Setup {
    implicit def mat: akka.stream.Materializer = app.injector.instanceOf[akka.stream.Materializer]

    val documentationController = app.injector.instanceOf[DocumentationController]
    val request = FakeRequest()
  }

  "microservice" should {
    "provide definition endpoint and documentation endpoint for each api" in new Setup {
      val result = documentationController.definition()(request)
      status(result) shouldBe OK
    }

    "provide raml documentation" in new Setup {
      val result = documentationController.specification("1.0", "application.yaml")(request)
      status(result) shouldBe OK
      contentAsString(result) should startWith("openapi:")
    }
  }
}
