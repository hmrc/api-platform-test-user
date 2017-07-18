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

package it.uk.gov.hmrc.testuser.services

import org.joda.time.LocalDate
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserMongoRepository
import uk.gov.hmrc.testuser.services.MigrationService

import scala.concurrent.ExecutionContext.Implicits.global

class MigrationServiceSpec extends UnitSpec with MongoSpecSupport with WithFakeApplication with BeforeAndAfterEach {

  val repository = new TestUserMongoRepository()

  trait Setup {
    val underTest = new MigrationService {
      override lazy val mongoConnector = mongoConnectorForTest
    }

    val jsonCollection = repository.collection
  }

  val individualDetails = IndividualDetails("John", "Doe", LocalDate.parse("1980-01-10"), Address("221b Baker St", "Marylebone", "NW1 6XE"))
  val individual = TestIndividual("userId", "password", individualDetails)

  val organisationDetails = OrganisationDetails("Company ABCDEF",  Address("225 Baker St", "Marylebone", "NW1 6XE"))
  val testOrganisation = TestOrganisation("userId", "password", organisationDetails)

  override def beforeEach() {
    await(repository.drop)
  }

  "migrate" should {

    "add postcode in existing individuals" in new Setup {

      await(jsonCollection.save(Json.parse(
        """
          |{
          | "userId": "userId",
          | "password": "password",
          | "individualDetails": {
          |   "firstName" : "John",
          |   "lastName" : "Doe",
          |   "dateOfBirth" : "1980-01-10",
          |   "address" : {
          |     "line1" : "221b Baker St",
          |     "line2" : "Marylebone"
          |   }
          | },
          | "userType": "INDIVIDUAL",
          | "services": []
          |}
        """.stripMargin
      )))

      await(underTest.migrate())

      val result = await(repository.fetchByUserId("userId")).map(_.asInstanceOf[TestIndividual])
      result.get.individualDetails.address.postcode should not be null
    }

    "add postcode in existing organisations" in new Setup {

      await(jsonCollection.save(Json.parse(
        """
          |{
          | "userId": "userId",
          | "password": "password",
          | "organisationDetails": {
          |   "name" : "Company ABCDEF 12345",
          |   "address" : {
          |     "line1" : "221b Baker St",
          |     "line2" : "Marylebone"
          |   }
          | },
          | "userType": "ORGANISATION",
          | "services": []
          |}
        """.stripMargin
      )))

      await(underTest.migrate())

      val result = await(repository.fetchByUserId("userId")).map(_.asInstanceOf[TestOrganisation])
      result.get.organisationDetails.address.postcode should not be null
    }
  }
}
