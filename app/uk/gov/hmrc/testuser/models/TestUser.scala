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

import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.domain._
import uk.gov.hmrc.testuser.models.UserType.UserType

sealed trait TestUser {
  val username: String
  val password: String
  val affinityGroup: String
  val _id: BSONObjectID
}

case class TestIndividual(override val username: String,
                          override val password: String,
                          saUtr: SaUtr,
                          nino: Nino,
                          override val _id: BSONObjectID = BSONObjectID.generate) extends TestUser {
  override val affinityGroup = "Individual"
}

case class TestOrganisation(override val username: String,
                            override val password: String,
                            saUtr: SaUtr,
                            empRef: EmpRef,
                            ctUtr: CtUtr,
                            vrn: Vrn,
                            override val _id: BSONObjectID = BSONObjectID.generate) extends TestUser {
  override val affinityGroup = "Organisation"
}

case class TestIndividualCreatedResponse(username: String, password: String, saUtr: SaUtr, nino: Nino)
case class TestOrganisationCreatedResponse(username: String, password: String, saUtr: SaUtr, empRef: EmpRef, ctUtr: CtUtr, vrn: Vrn)

object TestIndividualCreatedResponse {
  def from(individual: TestIndividual) = TestIndividualCreatedResponse(individual.username, individual.password, individual.saUtr, individual.nino)
}

object TestOrganisationCreatedResponse {
  def from(organisation: TestOrganisation) = TestOrganisationCreatedResponse(organisation.username, organisation.password, organisation.saUtr,
    organisation.empRef, organisation.ctUtr, organisation.vrn)
}

sealed trait TestUserResponse {
  val username: String
  val saUtr: SaUtr
  val userType: UserType
}

case class TestIndividualResponse(override val username: String,
                                  override val saUtr: SaUtr,
                                  nino: Nino,
                                  override val userType: UserType = UserType.INDIVIDUAL) extends TestUserResponse
case class TestOrganisationResponse(override val username: String,
                                    override val saUtr: SaUtr,
                                    empRef: EmpRef,
                                    ctUtr: CtUtr,
                                    vrn: Vrn,
                                    override val userType: UserType = UserType.ORGANISATION) extends TestUserResponse

object TestIndividualResponse {
  def from(individual: TestIndividual) = TestIndividualResponse(individual.username, individual.saUtr, individual.nino)
}

object TestOrganisationResponse {
  def from(organisation: TestOrganisation) = TestOrganisationResponse(organisation.username, organisation.saUtr,
    organisation.empRef, organisation.ctUtr, organisation.vrn)
}
