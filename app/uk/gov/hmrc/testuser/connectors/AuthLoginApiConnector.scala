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

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.http.HeaderNames.{AUTHORIZATION, LOCATION}
import play.api.libs.json.Json
import play.api.{Configuration, Environment}
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models.ServiceKey._
import uk.gov.hmrc.testuser.models._

@Singleton
class AuthLoginApiConnector @Inject() (
    httpClient: HttpClientV2,
    val configuration: Configuration,
    environment: Environment,
    config: ServicesConfig
  )(implicit ec: ExecutionContext
  ) {
  import config.baseUrl

  lazy val serviceUrl: String = baseUrl("auth-login-api")

  def createSession(testUser: TestUser)(implicit hc: HeaderCarrier): Future[AuthSession] = {

    httpClient.post(url"$serviceUrl/government-gateway/session/login")(hc.copy(authorization = None))
      .withBody(Json.toJson(GovernmentGatewayLogin(testUser)))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(response) =>
          (response.header(AUTHORIZATION), response.header(LOCATION)) match {
            case (Some(authBearerToken), Some(authorityUri)) =>
              val gatewayToken = (response.json \ "gatewayToken").as[String]
              AuthSession(authBearerToken, authorityUri, gatewayToken)
            case _                                           =>
              throw new RuntimeException("Authorization and Location header must be present in response.")
          }
        case Left(err)       => throw err
      }
  }
}

case class Identifier(key: String, value: String)

case class MdtpInformation(deviceId: String, sessionId: String)

object MdtpInformation {

  def fromHeaderCarrier(implicit hc: HeaderCarrier): MdtpInformation = {
    MdtpInformation(hc.deviceID.getOrElse("TestDeviceId"), hc.sessionId.map(_.value).getOrElse("TestSessionId"))
  }
}

case class Enrolment(key: String, identifiers: Seq[Identifier], state: String = "Activated")

case class GovernmentGatewayLogin(
    credId: String,
    affinityGroup: String,
    nino: Option[String],
    enrolments: Seq[Enrolment],
    usersName: String,
    email: String,
    confidenceLevel: Int,
    credentialStrength: String = "strong",
    groupIdentifier: String,
    itmpData: Option[ItmpData],
    mdtpInformation: Option[MdtpInformation] = None,
    credentialRole: Option[String] = None,
    agentCode: Option[String] = None
  )

case class ItmpData(
    givenName: String,
    middleName: String,
    familyName: String,
    birthdate: LocalDate,
    address: AuthLoginAddress
  )

object ItmpData {

  def apply(testIndividualDetails: IndividualDetails): ItmpData = {
    ItmpData(
      testIndividualDetails.firstName,
      middleName = "",
      testIndividualDetails.lastName,
      testIndividualDetails.dateOfBirth,
      address = AuthLoginAddress(testIndividualDetails.address)
    )
  }
}

case class AuthLoginAddress(
    line1: String,
    line2: String,
    postCode: String,
    countryName: String,
    countryCode: String
  )

object AuthLoginAddress {

  def apply(address: Address): AuthLoginAddress = {
    AuthLoginAddress(
      line1 = address.line1,
      line2 = address.line2,
      postCode = address.postcode,
      countryName = "United Kingdom",
      countryCode = "GB"
    )
  }
}

object GovernmentGatewayLogin {
  import com.typesafe.config.ConfigFactory
  lazy val confidenceLevel: Int = ConfigFactory.load().getInt("confidenceLevel")

  def apply(testUser: TestUser)(implicit hc: HeaderCarrier): GovernmentGatewayLogin = testUser match {
    case individual: TestIndividual     => fromIndividual(individual)
    case organisation: TestOrganisation => fromOrganisation(organisation)
    case agent: TestAgent               => fromAgent(agent)
  }

  private def fromIndividual(individual: TestIndividual)(implicit hc: HeaderCarrier): GovernmentGatewayLogin = {

    def asEnrolment(serviceName: ServiceKey) = {
      serviceName match {
        case SELF_ASSESSMENT             => individual.saUtr map { saUtr => Enrolment("IR-SA", Seq(Identifier("UTR", saUtr))) }
        case MTD_INCOME_TAX              => individual.mtdItId map { mtdItId => Enrolment("HMRC-MTD-IT", Seq(Identifier("MTDITID", mtdItId))) }
        case CUSTOMS_SERVICES            => individual.eoriNumber map { eoriNumber => Enrolment("HMRC-CUS-ORG", Seq(Identifier("EORINumber", eoriNumber))) }
        case GOODS_VEHICLE_MOVEMENTS     => individual.eoriNumber map { eoriNumber => Enrolment("HMRC-GVMS-ORG", Seq(Identifier("EORINumber", eoriNumber))) }
        case MTD_VAT                     => individual.vrn map { vrn => Enrolment("HMRC-MTD-VAT", Seq(Identifier("VRN", vrn))) }
        case CTC_LEGACY                  => individual.eoriNumber map { eoriNumber => Enrolment("HMCE-NCTS-ORG", Seq(Identifier("VATRegNoTURN", eoriNumber))) }
        case CTC                         => individual.eoriNumber map { eoriNumber => Enrolment("HMRC-CTC-ORG", Seq(Identifier("EORINumber", eoriNumber))) }
        case IMPORT_CONTROL_SYSTEM       => individual.eoriNumber map { eoriNumber => Enrolment("HMRC-ICS-ORG", Seq(Identifier("EoriTin", eoriNumber))) }
        case TAX_FREE_CHILDCARE_PAYMENTS => individual.nino map { nino => Enrolment("HMRC-TFCP-ORG", Seq(Identifier("nino", nino))) }
        case _                           => None
      }
    }

    GovernmentGatewayLogin(
      credId = individual.userId,
      affinityGroup = individual.affinityGroup,
      nino = individual.nino,
      enrolments = individual.services.flatMap(asEnrolment),
      usersName = individual.userFullName,
      email = individual.emailAddress,
      confidenceLevel = confidenceLevel,
      groupIdentifier = individual.groupIdentifier.getOrElse(""),
      itmpData = Some(ItmpData(individual.individualDetails)),
      mdtpInformation = Some(MdtpInformation.fromHeaderCarrier)
    )
  }

  private def fromOrganisation(organisation: TestOrganisation)(implicit hc: HeaderCarrier): GovernmentGatewayLogin = {

    def asEnrolment(serviceName: ServiceKey) = {
      serviceName match {
        case SELF_ASSESSMENT                 => organisation.saUtr map { saUtr => Enrolment("IR-SA", Seq(Identifier("UTR", saUtr))) }
        case CORPORATION_TAX                 => organisation.ctUtr map { ctUtr => Enrolment("IR-CT", Seq(Identifier("UTR", ctUtr))) }
        case SUBMIT_VAT_RETURNS              => organisation.vrn map { vrn => Enrolment("HMCE-VATDEC-ORG", Seq(Identifier("VATRegNo", vrn))) }
        case PAYE_FOR_EMPLOYERS              => organisation.empRef map { empRef =>
            val ref = EmpRef.fromIdentifiers(empRef)
            Enrolment("IR-PAYE", Seq(Identifier("TaxOfficeNumber", ref.taxOfficeNumber), Identifier("TaxOfficeReference", ref.taxOfficeReference)))
          }
        case MTD_INCOME_TAX                  => organisation.mtdItId map { mtdItId => Enrolment("HMRC-MTD-IT", Seq(Identifier("MTDITID", mtdItId))) }
        case MTD_VAT                         => organisation.vrn map { vrn => Enrolment("HMRC-MTD-VAT", Seq(Identifier("VRN", vrn.toString()))) }
        case LISA                            => organisation.lisaManRefNum map {
            lisaManRefNum => Enrolment("HMRC-LISA-ORG", Seq(Identifier("ZREF", lisaManRefNum)))
          }
        case SECURE_ELECTRONIC_TRANSFER      => organisation.secureElectronicTransferReferenceNumber map {
            setRefNum => Enrolment("HMRC-SET-ORG", Seq(Identifier("SRN", setRefNum)))
          }
        case RELIEF_AT_SOURCE                => organisation.pensionSchemeAdministratorIdentifier map {
            psaId => Enrolment("HMRC-PSA-ORG", Seq(Identifier("PSAID", psaId)))
          }
        case CUSTOMS_SERVICES                => organisation.eoriNumber map { eoriNumber => Enrolment("HMRC-CUS-ORG", Seq(Identifier("EORINumber", eoriNumber))) }
        case CTC_LEGACY                      => organisation.eoriNumber map { eoriNumber => Enrolment("HMCE-NCTS-ORG", Seq(Identifier("VATRegNoTURN", eoriNumber))) }
        case CTC                             => organisation.eoriNumber map { eoriNumber => Enrolment("HMRC-CTC-ORG", Seq(Identifier("EORINumber", eoriNumber))) }
        case EMCS                            => organisation.exciseNumber map { exciseNumber => Enrolment("HMRC-EMCS-ORG", Seq(Identifier("ExciseNumber", exciseNumber))) }
        case GOODS_VEHICLE_MOVEMENTS         => organisation.eoriNumber map { eoriNumber =>
            Enrolment("HMRC-GVMS-ORG", Seq(Identifier("EORINumber", eoriNumber)))
          }
        case SAFETY_AND_SECURITY             => organisation.eoriNumber map { eoriNumber => Enrolment("HMRC-SS-ORG", Seq(Identifier("EORINumber", eoriNumber))) }
        case IMPORT_CONTROL_SYSTEM           => organisation.eoriNumber map { eoriNumber => Enrolment("HMRC-ICS-ORG", Seq(Identifier("EoriTin", eoriNumber))) }
        case MODERNISATION_OF_AUTHORISATIONS => organisation.eoriNumber map { eoriNumber => Enrolment("HMRC-MA-ORG", Seq(Identifier("EORINumber", eoriNumber))) }
        case PILLAR_2                        => organisation.pillar2Id map { pillar2Id => Enrolment("HMRC-PILLAR2-ORG", Seq(Identifier("PLRID", pillar2Id))) }
        case _                               => None
      }
    }

    GovernmentGatewayLogin(
      credId = organisation.userId,
      affinityGroup = organisation.affinityGroup,
      nino = organisation.nino,
      enrolments = organisation.services.flatMap(asEnrolment),
      usersName = organisation.userFullName,
      email = organisation.emailAddress,
      confidenceLevel = confidenceLevel,
      groupIdentifier = organisation.groupIdentifier.getOrElse(""),
      itmpData = organisation.individualDetails.map(ItmpData(_)),
      mdtpInformation = Some(MdtpInformation.fromHeaderCarrier)
    )
  }

  private def fromAgent(agent: TestAgent)(implicit hc: HeaderCarrier): GovernmentGatewayLogin = {
    def asEnrolment(serviceName: ServiceKey) = {
      serviceName match {
        case AGENT_SERVICES => agent.arn map { arn => Enrolment("HMRC-AS-AGENT", Seq(Identifier("AgentReferenceNumber", arn))) }
        case _              => None
      }
    }

    GovernmentGatewayLogin(
      credId = agent.userId,
      affinityGroup = agent.affinityGroup,
      nino = None,
      enrolments = agent.services.flatMap(asEnrolment),
      usersName = agent.userFullName,
      email = agent.emailAddress,
      confidenceLevel = confidenceLevel,
      credentialRole = Some("user"),
      groupIdentifier = agent.groupIdentifier.getOrElse(""),
      itmpData = None,
      agentCode = agent.agentCode,
      mdtpInformation = Some(MdtpInformation.fromHeaderCarrier)
    )
  }
}
