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

package unit.uk.gov.hmrc.testuser.connectors

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.connectors.{ServiceLocatorConfig, ServiceLocatorConnector}
import uk.gov.hmrc.testuser.models.Registration

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ServiceLocatorConnectorSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val appName = "api-microservice"
  val appUrl = "http://example.com"
  val metadata = Some(Map("third-party-api" -> "true"))
  val serviceLocatorBaseUrl = "https://SERVICE_LOCATOR"
  val serviceLocatorUrl = s"$serviceLocatorBaseUrl/registration"
  val serviceLocatorConfig = ServiceLocatorConfig(appName, appUrl, serviceLocatorBaseUrl)

  val expectedRegistration = Registration(appName, appUrl, metadata)
  val expectedContentTypeHeader = "Content-Type" -> "application/json"

  trait Setup {
    val serviceLocatorException = new RuntimeException

    Logger.info("Waking up the logger to avoid timeout")

    val httpClientMock = mock[HttpClient]
    val connector = new ServiceLocatorConnector(
      httpClientMock,
      serviceLocatorConfig
    )

    def serviceLocatorWillRespondWith(response: Future[HttpResponse]): Unit = {
      when(httpClientMock.POST(
        any[String](), any[Registration](), any[Seq[(String, String)]]())(any[Writes[Registration]](), any[HttpReads[HttpResponse]](), any(), any())
      ).thenReturn(Future.successful(response))
    }

    def verifyRegistrationSuccessful(result: Future[Boolean]): Unit = verifyRegistration(result, expected = true)

    def verifyRegistrationFailed(result: Future[Boolean]): Unit = verifyRegistration(result, expected = false)

    def verifyRegistration(result: Future[Boolean], expected: Boolean): Unit = {
      result.futureValue shouldBe expected
      verify(httpClientMock).POST(meq(serviceLocatorUrl), meq(expectedRegistration), meq(Seq(expectedContentTypeHeader))
      )(any[Writes[Registration]](), any[HttpReads[HttpResponse]](), any(), any())
    }
  }

  "register" should {

    "register the JSON API Definition into the Service Locator" in new Setup {

      serviceLocatorWillRespondWith(Future.successful(HttpResponse(OK)))

      val result = connector.register()

      verifyRegistrationSuccessful(result)
    }

    "fail registering in service locator" in new Setup {

      serviceLocatorWillRespondWith(Future.failed(serviceLocatorException))

      val result = connector.register()

      verifyRegistrationFailed(result)
    }

  }
}

