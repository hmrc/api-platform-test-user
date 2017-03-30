/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.Singleton

import play.api.http.HeaderNames.{AUTHORIZATION, LOCATION}
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.testuser.config.WSHttp
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AuthLoginApiConnector extends ServicesConfig {
  lazy val serviceUrl: String = baseUrl("auth-login-api")

  def createSession(testUser: TestUser)(implicit hc: HeaderCarrier): Future[AuthSession] = {
    WSHttp.POST(s"$serviceUrl/government-gateway/legacy/login", GovernmentGatewayLogin(testUser)) map { response =>
      val gatewayToken = (response.json \ "gatewayToken").as[String]

      (response.header(AUTHORIZATION), response.header(LOCATION)) match {
        case (Some(authBearerToken), Some(authorityUri)) => AuthSession(authBearerToken, authorityUri, gatewayToken)
        case _ =>  throw new RuntimeException(s"Authorization and Location header must be present in response.")
      }
    }
  }
}

case class TaxIdentifier(key: String, value: String)
case class Enrolment(key: String, identifiers: Seq[TaxIdentifier], state: String = "Activated")

case class GovernmentGatewayLogin(credId: String,
                                  affinityGroup: String,
                                  nino: Option[String],
                                  enrolments: Seq[Enrolment],
                                  confidenceLevel: Int = ConfidenceLevel.L200.level,
                                  credentialStrength: String = "strong"
                                 )

object GovernmentGatewayLogin {
  def apply(testUser: TestUser): GovernmentGatewayLogin = testUser match {
    case individual: TestIndividual =>
      GovernmentGatewayLogin(individual.userId, testUser.affinityGroup, Some(individual.nino.value), Seq(
        Enrolment("IR-SA", utr(individual.saUtr.toString))))

    case organisation: TestOrganisation =>
      GovernmentGatewayLogin(organisation.userId, testUser.affinityGroup, None, Seq(
        Enrolment("IR-SA", utr(organisation.saUtr.toString)),
        Enrolment("IR-CT", utr(organisation.ctUtr.toString)),
        Enrolment("HMCE-VATDEC-ORG", vrn(organisation.vrn.toString)),
        Enrolment("IR-PAYE", paye(organisation.empRef))))
  }

  private def utr(saUtr: String) = Seq(TaxIdentifier("UTR", saUtr))
  private def vrn(vrn: String) = Seq(TaxIdentifier("VATRegNo", vrn))
  private def paye(empRef: EmpRef) = Seq(
    TaxIdentifier("TaxOfficeNumber", empRef.taxOfficeNumber),
    TaxIdentifier("TaxOfficeReference", empRef.taxOfficeReference))
}
