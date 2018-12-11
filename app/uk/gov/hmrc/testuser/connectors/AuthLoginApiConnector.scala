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

import javax.inject.Singleton

import play.api.http.HeaderNames.{AUTHORIZATION, LOCATION}
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.testuser.config.WSHttp
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models.ServiceName._
import uk.gov.hmrc.testuser.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AuthLoginApiConnector extends ServicesConfig {

  lazy val serviceUrl: String = baseUrl("auth-login-api")

  def createSession(testUser: TestUser)
                   (implicit hc: HeaderCarrier): Future[AuthSession] = {
    WSHttp.POST(s"$serviceUrl/government-gateway/session/login", GovernmentGatewayLogin(testUser)) map { response =>
      val gatewayToken = (response.json \ "gatewayToken").as[String]

      (response.header(AUTHORIZATION), response.header(LOCATION)) match {
        case (Some(authBearerToken), Some(authorityUri)) => AuthSession(authBearerToken, authorityUri, gatewayToken)
        case _ => throw new RuntimeException("Authorization and Location header must be present in response.")
      }
    }
  }

}

case class Identifier(key: String, value: String)

case class Enrolment(key: String, identifiers: Seq[Identifier], state: String = "Activated")

case class GovernmentGatewayLogin(credId: String,
                                  affinityGroup: String,
                                  nino: Option[String],
                                  enrolments: Seq[Enrolment],
                                  usersName: String,
                                  email: String,
                                  confidenceLevel: Int = ConfidenceLevel.L200.level,
                                  credentialStrength: String = "strong",
                                  credentialRole: Option[String] = None)

object GovernmentGatewayLogin {

  def apply(testUser: TestUser): GovernmentGatewayLogin = testUser match {
    case individual: TestIndividual => fromIndividual(individual)
    case organisation: TestOrganisation => fromOrganisation(organisation)
    case agent: TestAgent => fromAgent(agent)
  }

  private def fromIndividual(individual: TestIndividual) = {

    def asEnrolment(serviceName: ServiceName) = {
      serviceName match {
        case SELF_ASSESSMENT => individual.saUtr map { saUtr => Enrolment("IR-SA", taxIdentifier(saUtr)) }
        case MTD_INCOME_TAX => individual.mtdItId map { mtdItId => Enrolment("HMRC-MTD-IT", taxIdentifier(mtdItId)) }
        case CUSTOMS_SERVICES => individual.eoriNumber map { eoriNumber => Enrolment("HMRC-CUS-ORG", taxIdentifier(eoriNumber)) }
        case MTD_VAT => individual.vrn map {vrn => Enrolment("HMRC-MTD-VAT", Seq(Identifier("VRN", vrn.toString()))) }
        case _ => None
      }
    }

    GovernmentGatewayLogin(individual.userId, individual.affinityGroup, individual.nino.map(_.value),
      individual.services.flatMap(asEnrolment), individual.userFullName, individual.emailAddress)
  }

  private def fromOrganisation(organisation: TestOrganisation) = {

    def asEnrolment(serviceName: ServiceName) = {
      serviceName match {
        case SELF_ASSESSMENT => organisation.saUtr map { saUtr => Enrolment("IR-SA", taxIdentifier(saUtr)) }
        case CORPORATION_TAX => organisation.ctUtr map { ctUtr => Enrolment("IR-CT", taxIdentifier(ctUtr)) }
        case SUBMIT_VAT_RETURNS => organisation.vrn map { vrn => Enrolment("HMCE-VATDEC-ORG", taxIdentifier(vrn)) }
        case PAYE_FOR_EMPLOYERS => organisation.empRef map { empRef => Enrolment("IR-PAYE", taxIdentifier(empRef)) }
        case MTD_INCOME_TAX => organisation.mtdItId map { mtdItId => Enrolment("HMRC-MTD-IT", taxIdentifier(mtdItId)) }
        case MTD_VAT => organisation.vrn map {vrn => Enrolment("HMRC-MTD-VAT", Seq(Identifier("VRN", vrn.toString()))) }
        case LISA => organisation.lisaManRefNum map { lisaManRefNum => Enrolment("HMRC-LISA-ORG", taxIdentifier(lisaManRefNum)) }
        case SECURE_ELECTRONIC_TRANSFER => organisation.secureElectronicTransferReferenceNumber map { setRefNum => Enrolment("HMRC-SET-ORG", taxIdentifier(setRefNum)) }
        case RELIEF_AT_SOURCE => organisation.pensionSchemeAdministratorIdentifier map { psaId => Enrolment("HMRC-PSA-ORG", taxIdentifier(psaId)) }
        case CUSTOMS_SERVICES => organisation.eoriNumber map { eoriNumber => Enrolment("HMRC-CUS-ORG", taxIdentifier(eoriNumber)) }
        case _ => None
      }
    }

    GovernmentGatewayLogin(organisation.userId, organisation.affinityGroup, organisation.nino.map(_.value),
      organisation.services.flatMap(asEnrolment), organisation.userFullName, organisation.emailAddress)
  }

  private def fromAgent(agent: TestAgent) = {
    def asEnrolment(serviceName: ServiceName) = {
      serviceName match {
        case AGENT_SERVICES => agent.arn map { arn => Enrolment("HMRC-AS-AGENT", taxIdentifier(arn)) }
        case _ => None
      }
    }

    GovernmentGatewayLogin(agent.userId, agent.affinityGroup, None, agent.services.flatMap(asEnrolment),
      agent.userFullName, agent.emailAddress,
      credentialRole = Some("user"))
  }

  private def taxIdentifier(taxIdentifier: TaxIdentifier) = {
    taxIdentifier match {
      case saUtr: SaUtr => Seq(Identifier("UTR", saUtr.toString))
      case ctUtr: CtUtr => Seq(Identifier("UTR", ctUtr.toString))
      case vrn: Vrn => Seq(Identifier("VATRegNo", vrn.toString))
      case empRef: EmpRef => Seq(Identifier("TaxOfficeNumber", empRef.taxOfficeNumber),
        Identifier("TaxOfficeReference", empRef.taxOfficeReference))
      case arn: AgentBusinessUtr => Seq(Identifier("AgentReferenceNumber", arn.toString))
      case mtdItId: MtdItId => Seq(Identifier("MTDITID", mtdItId.toString))
      case lisaManRefNum: LisaManagerReferenceNumber => Seq(Identifier("ZREF", lisaManRefNum.toString))
      case setRefNum: SecureElectronicTransferReferenceNumber => Seq(Identifier("SRN", setRefNum.toString))
      case psaId: PensionSchemeAdministratorIdentifier => Seq(Identifier("PSAID", psaId.toString))
      case EoriNumber(value) => Seq(Identifier("EORINumber", value))
      case _ => Seq.empty
    }
  }

}
