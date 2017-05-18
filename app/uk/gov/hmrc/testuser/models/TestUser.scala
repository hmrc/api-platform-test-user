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

package uk.gov.hmrc.testuser.models

import play.api.libs.json.{Format, Reads, Writes}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.domain._
import uk.gov.hmrc.testuser.models.ServiceName.ServiceName
import uk.gov.hmrc.testuser.models.UserType.UserType

object ServiceName extends Enumeration {
  type ServiceName = Value
  val NATIONAL_INSURANCE = Value("national-insurance")
  val SELF_ASSESSMENT = Value("self-assessment")
  val CORPORATION_TAX = Value("corporation-tax")
  val PAYE_FOR_EMPLOYERS = Value("paye-for-employers")
  val SUBMIT_VAT_RETURNS = Value("submit-vat-returns")
  val MTD_INCOME_TAX = Value("mtd-income-tax")
  val AGENT_SERVICES = Value("agent-services")
  val LISA = Value("lisa")
}

sealed trait TestUser {
  val userId: String
  val password: String
  val affinityGroup: String
  val services: Seq[ServiceName]
  val _id: BSONObjectID
}

case class TestIndividual(override val userId: String,
                          override val password: String,
                          saUtr: Option[SaUtr] = None,
                          nino: Option[Nino] = None,
                          mtdItId: Option[MtdItId] = None,
                          override val services: Seq[ServiceName] = Seq.empty,
                          override val _id: BSONObjectID = BSONObjectID.generate) extends TestUser {
  override val affinityGroup = "Individual"
}

case class TestOrganisation(override val userId: String,
                            override val password: String,
                            saUtr: Option[SaUtr] = None,
                            nino: Option[Nino] = None,
                            mtdItId: Option [MtdItId] = None,
                            empRef: Option[EmpRef] = None,
                            ctUtr: Option[CtUtr] = None,
                            vrn: Option[Vrn] = None,
                            lisaManRefNum: Option[LisaManagerReferenceNumber] = None,
                            override val services: Seq[ServiceName] = Seq.empty,
                            override val _id: BSONObjectID = BSONObjectID.generate) extends TestUser {
  override val affinityGroup = "Organisation"
}

case class TestAgent(override val userId: String,
                     override val password: String,
                     arn: Option[AgentBusinessUtr] = None,
                     override val services: Seq[ServiceName] = Seq.empty,
                     override val _id: BSONObjectID = BSONObjectID.generate) extends TestUser {
  override val affinityGroup = "Agent"
}

case class TestIndividualCreatedResponse(userId: String, password: String, saUtr: Option[SaUtr], nino: Option[Nino], mtdItId: Option[MtdItId])
case class TestOrganisationCreatedResponse(userId: String, password: String, saUtr: Option[SaUtr], nino: Option[Nino], mtdItId: Option[MtdItId],
                                           empRef: Option[EmpRef], ctUtr: Option[CtUtr], vrn: Option[Vrn])
case class TestAgentCreatedResponse(userId: String, password: String, arn: Option[AgentBusinessUtr])

object TestIndividualCreatedResponse {
  def from(individual: TestIndividual) = TestIndividualCreatedResponse(individual.userId, individual.password,
    individual.saUtr, individual.nino, individual.mtdItId)
}

object TestOrganisationCreatedResponse {
  def from(organisation: TestOrganisation) = TestOrganisationCreatedResponse(organisation.userId, organisation.password,
    organisation.saUtr, organisation.nino, organisation.mtdItId,
    organisation.empRef, organisation.ctUtr, organisation.vrn)
}

object TestAgentCreatedResponse {
  def from(agent: TestAgent) = TestAgentCreatedResponse(agent.userId, agent.password, agent.arn)
}

sealed trait TestUserResponse {
  val userId: String
  val saUtr: Option[SaUtr]
  val nino: Option[Nino]
  val mtdItId: Option[MtdItId]
  val userType: UserType
}

case class TestIndividualResponse(override val userId: String,
                                  override val saUtr: Option[SaUtr] = None,
                                  override val nino: Option[Nino] = None,
                                  override val mtdItId: Option[MtdItId] = None,
                                  override val userType: UserType = UserType.INDIVIDUAL) extends TestUserResponse

case class TestOrganisationResponse(override val userId: String,
                                    override val saUtr: Option[SaUtr] = None,
                                    override val nino: Option[Nino] = None,
                                    override val mtdItId: Option[MtdItId] = None,
                                    empRef: Option[EmpRef] = None,
                                    ctUtr: Option[CtUtr] = None,
                                    vrn: Option[Vrn] = None,
                                    lisaManagerReferenceNumber: Option[LisaManagerReferenceNumber] = None,
                                    override val userType: UserType = UserType.ORGANISATION) extends TestUserResponse

case class TestAgentResponse(userId: String,
                             arn: Option[AgentBusinessUtr] = None,
                             userType: UserType = UserType.AGENT)

object TestIndividualResponse {
  def from(individual: TestIndividual) = TestIndividualResponse(individual.userId, individual.saUtr, individual.nino,
    individual.mtdItId)
}

object TestOrganisationResponse {
  def from(organisation: TestOrganisation) = TestOrganisationResponse(organisation.userId, organisation.saUtr,
    organisation.nino, organisation.mtdItId, organisation.empRef, organisation.ctUtr, organisation.vrn, organisation.lisaManRefNum)
}

object TestAgentResponse {
  def from(agent: TestAgent) = TestAgentResponse(agent.userId, agent.arn)
}

case class DesSimulatorTestIndividual(val mtdItId: Option[MtdItId], val nino: Option[Nino], val saUtr: Option[SaUtr])

object DesSimulatorTestIndividual {
  def from(individual: TestIndividual) = DesSimulatorTestIndividual(individual.mtdItId, individual.nino, individual.saUtr)
}

case class DesSimulatorTestOrganisation(val mtdItId: Option[MtdItId], val nino: Option[Nino],
                                        val saUtr: Option[SaUtr], val ctUtr: Option[CtUtr],
                                        val empRef: Option[EmpRef], val vrn: Option[Vrn])

object DesSimulatorTestOrganisation {
  def from(organisation: TestOrganisation) = DesSimulatorTestOrganisation(organisation.mtdItId,
    organisation.nino, organisation.saUtr, organisation.ctUtr, organisation.empRef, organisation.vrn)
}

case class MtdItId(mtdItId: String) extends TaxIdentifier with SimpleName {
  require(MtdItId.isValid(mtdItId), s"$mtdItId is not a valid MTDITID.")
  override def toString = mtdItId

  def value = mtdItId

  val name = "mtdItId"

  def formatted = value
}


object MtdItId extends Modulus23Check with (String => MtdItId) {
  implicit val mtdItIdWrite: Writes[MtdItId] = new SimpleObjectWrites[MtdItId](_.value)
  implicit val mtdItIdRead: Reads[MtdItId] = new SimpleObjectReads[MtdItId]("mtdItId", MtdItId.apply)
  implicit val mtdItIdFormat: Format[MtdItId] = Format(mtdItIdRead, mtdItIdWrite)

  private val validMtdItIdFormat = "^X[A-Z]IT[0-9]{11}$"

  def isValid(mtdItId: String) = {
    mtdItId.matches(validMtdItIdFormat) && isCheckCorrect(mtdItId, 1)
  }

}

case class LisaManagerReferenceNumber(lisaManagerReferenceNumber: String) extends TaxIdentifier with SimpleName {
  override def toString = lisaManagerReferenceNumber
  val name = "lisaManagerReferenceNumber"
  def value = lisaManagerReferenceNumber
}

object LisaManagerReferenceNumber extends (String => LisaManagerReferenceNumber) {
  implicit val lisaManRefNumWrite: Writes[LisaManagerReferenceNumber] = new SimpleObjectWrites[LisaManagerReferenceNumber](_.value)
  implicit val lisaManRefNumRead: Reads[LisaManagerReferenceNumber] = new SimpleObjectReads[LisaManagerReferenceNumber]("lisaManagerReferenceNumber", LisaManagerReferenceNumber.apply)
}