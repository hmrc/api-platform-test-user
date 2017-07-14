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

package it.uk.gov.hmrc.testuser.repository

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import uk.gov.hmrc.domain._
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.models.ServiceName._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserMongoRepository
import uk.gov.hmrc.testuser.services.Generator._

import scala.concurrent.ExecutionContext.Implicits.global

class TestUserRepositorySpec extends UnitSpec with BeforeAndAfterEach with BeforeAndAfterAll with MongoSpecSupport {

  private val repository = new TestUserMongoRepository
  val testIndividual = generateTestIndividual(Seq(MTD_INCOME_TAX, SELF_ASSESSMENT, NATIONAL_INSURANCE))
  val testOrganisation = generateTestOrganisation(Seq(MTD_INCOME_TAX, SELF_ASSESSMENT, NATIONAL_INSURANCE, CORPORATION_TAX, PAYE_FOR_EMPLOYERS))

  override def beforeEach() {
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  override protected def afterAll() {
    await(repository.drop)
  }

  "createUser" should {

    "create a test individual in the repository" in {

      val result = await(repository.createUser(testIndividual))

      result shouldBe testIndividual
      await(repository.findById(testIndividual._id)) shouldBe Some(testIndividual)
    }

    "create a test organisation in the repository" in {

      val result = await(repository.createUser(testOrganisation))

      result shouldBe testOrganisation
      await(repository.findById(testOrganisation._id)) shouldBe Some(testOrganisation)
    }
  }

  "fetchByUserId" should {

    "return an individual when the individual exists for the userId" in {

      await(repository.createUser(testIndividual))

      val result = await(repository.fetchByUserId(testIndividual.userId))

      result shouldBe Some(testIndividual)
    }

    "return an organisation when the organisation exists for the userId" in {

      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchByUserId(testOrganisation.userId))

      result shouldBe Some(testOrganisation)
    }

    "return None when no user matches the userId" in {

      val result = await(repository.fetchByUserId("unknown"))

      result shouldBe None
    }
  }

  "fetchIndividualByNino" should {

    "return the individual" in {
      await(repository.createUser(testIndividual))

      val result = await(repository.fetchIndividualByNino(testIndividual.nino.get))

      result shouldBe Some(testIndividual)
    }

    "return None when there is no individual matching" in {
      val result = await(repository.fetchIndividualByNino(Nino("CC333333C")))

      result shouldBe None
    }
  }

  "fetchIndividualByShortNino" should {
    val nino = Nino("CC333333C")
    val shortNino = NinoNoSuffix("CC333333")
    val individual = testIndividual.copy(nino = Some(nino))

    "return the individual" in {
      await(repository.createUser(individual))

      val result = await(repository.fetchIndividualByShortNino(shortNino))

      result shouldBe Some(individual)
    }

    "return None when there is no individual matching" in {
      val result = await(repository.fetchIndividualByShortNino(shortNino))

      result shouldBe None
    }
  }

  "fetchIndividualBySautr" should {

    "return the individual" in {
      await(repository.createUser(testIndividual))

      val result = await(repository.fetchIndividualBySaUtr(testIndividual.saUtr.get))

      result shouldBe Some(testIndividual)
    }

    "return None when there is an organisation matching" in {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchIndividualBySaUtr(testOrganisation.saUtr.get))

      result shouldBe None
    }

    "return None when there is no individual matching" in {
      val result = await(repository.fetchIndividualBySaUtr(SaUtr("1555369052")))

      result shouldBe None
    }
  }

  "fetchOrganisationByEmpRef" should {

    "return the organisation" in {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchOrganisationByEmpRef(testOrganisation.empRef.get))

      result shouldBe Some(testOrganisation)
    }

    "return None when there is an organisation matching" in {
      val result = await(repository.fetchOrganisationByEmpRef(testOrganisation.empRef.get))

      result shouldBe None
    }
  }
}
