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

package it.uk.gov.hmrc.testuser.repository

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import uk.gov.hmrc.domain._
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.models.ServiceKeys._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.services.Generator

import scala.concurrent.ExecutionContext.Implicits.global

class TestUserRepositorySpec extends UnitSpec with BeforeAndAfterEach with BeforeAndAfterAll with MongoSpecSupport with IndexVerification {
  private val mongoComponent = new ReactiveMongoComponent {
    override def mongoConnector = mongoConnectorForTest
  }
  val repository = new TestUserRepository(mongoComponent)
  val generator = new Generator(repository)

  trait GeneratedTestIndividual {
    val testIndividual = await(generator.generateTestIndividual(Seq(MTD_INCOME_TAX, SELF_ASSESSMENT, NATIONAL_INSURANCE, MTD_VAT, CUSTOMS_SERVICES)))
  }

  trait GeneratedTestOrganisation {
    val testOrganisation =
      await(
        generator.generateTestOrganisation(
          Seq(MTD_INCOME_TAX, SELF_ASSESSMENT, NATIONAL_INSURANCE, CORPORATION_TAX, PAYE_FOR_EMPLOYERS, MTD_VAT, LISA, CUSTOMS_SERVICES)))
  }

  override def afterEach: Unit = {
    repository.removeAll()
  }

  "indexes" should {
    "be created for userId" in {
      val expectedIndex = Set(Index(key = Seq("userId" -> Ascending), name = Some("userIdIndex"), unique = true, background = true))
      verifyIndexesVersionAgnostic(repository, expectedIndex)
    }

    "be created for all identifier fields" in {
      def expectedIndexes: Set[Index] =
        repository.IdentifierFields
          .map(identifierField => Index(key = Seq(identifierField -> Ascending), name = Some(s"$identifierField-Index"), unique = false, background = true))
          .toSet

      verifyIndexesVersionAgnostic(repository, expectedIndexes)
    }
  }

  "createUser" should {

    "create a test individual in the repository" in new GeneratedTestIndividual {
      val result = await(repository.createUser(testIndividual))

      result shouldBe testIndividual
      await(repository.findById(testIndividual._id)) shouldBe Some(testIndividual)
    }

    "create a test organisation in the repository" in new GeneratedTestOrganisation {
      val result = await(repository.createUser(testOrganisation))

      result shouldBe testOrganisation
      await(repository.findById(testOrganisation._id)) shouldBe Some(testOrganisation)
    }
  }

  "fetchByUserId" should {
    "return an individual when the individual exists for the userId" in new GeneratedTestIndividual {
      await(repository.createUser(testIndividual))

      val result = await(repository.fetchByUserId(testIndividual.userId))

      result shouldBe Some(testIndividual)
    }

    "return an organisation when the organisation exists for the userId" in new GeneratedTestOrganisation {

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

    "return the individual" in new GeneratedTestIndividual {
      await(repository.createUser(testIndividual))

      val result = await(repository.fetchIndividualByNino(testIndividual.nino.get))

      result shouldBe Some(testIndividual)
    }

    "return None when there is no individual matching" in {
      val result = await(repository.fetchIndividualByNino(Nino("CC333334C")))

      result shouldBe None
    }

    "return None when there is an organisation matching" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchIndividualByNino(testOrganisation.nino.get))

      result shouldBe None
    }
  }

  "fetchIndividualByShortNino" should {
    val nino = Nino("CC333333C")
    val validShortNino = NinoNoSuffix("CC333333")
    val invalidShortNino = NinoNoSuffix("CC333334")

    "return the individual" in new GeneratedTestIndividual {
      val individual = testIndividual.copy(nino = Some(nino))
      await(repository.createUser(individual))

      val result = await(repository.fetchIndividualByShortNino(validShortNino))

      result shouldBe Some(individual)
    }

    "return None when there is no individual matching" in {
      val result = await(repository.fetchIndividualByShortNino(invalidShortNino))

      result shouldBe None
    }

    "return None when there is an organisation matching" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchIndividualByShortNino(NinoNoSuffix(testOrganisation.nino.get)))

      result shouldBe None
    }
  }

  "fetchIndividualBySaUtr" should {

    "return the individual" in new GeneratedTestIndividual {
      await(repository.createUser(testIndividual))

      val result = await(repository.fetchIndividualBySaUtr(testIndividual.saUtr.get))

      result shouldBe Some(testIndividual)
    }

    "return None when there is an organisation matching" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchIndividualBySaUtr(testOrganisation.saUtr.get))

      result shouldBe None
    }

    "return None when there is no individual matching" in {
      val result = await(repository.fetchIndividualBySaUtr(SaUtr("1555369052")))

      result shouldBe None
    }
  }

  "fetchIndividualByVrn" should {

    "return the individual" in new GeneratedTestIndividual {
      await(repository.createUser(testIndividual))

      val result = await(repository.fetchIndividualByVrn(testIndividual.vrn.get))

      result shouldBe Some(testIndividual)
    }

    "return None when there is an organisation matching" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchIndividualByVrn(testOrganisation.vrn.get))

      result shouldBe None
    }

    "return None when there is no individual matching" in {
      val result = await(repository.fetchIndividualByVrn(Vrn("1555369052")))

      result shouldBe None
    }
  }

  "fetchOrganisationByEmpRef" should {

    "return the organisation" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchOrganisationByEmpRef(testOrganisation.empRef.get))

      result shouldBe Some(testOrganisation)
    }

    "return None when there is an organisation matching" in new GeneratedTestOrganisation {
      val result = await(repository.fetchOrganisationByEmpRef(testOrganisation.empRef.get))

      result shouldBe None
    }
  }

  "fetchOrganisationByVrn" should {

    "return the organisation" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchOrganisationByVrn(testOrganisation.vrn.get))

      result shouldBe Some(testOrganisation)
    }

    "return None when there is an individual matching" in new GeneratedTestIndividual {
      await(repository.createUser(testIndividual))

      val result = await(repository.fetchOrganisationByVrn(testIndividual.vrn.get))

      result shouldBe None
    }

    "return None when there is an organisation matching" in new GeneratedTestOrganisation {
      val result = await(repository.fetchOrganisationByVrn(testOrganisation.vrn.get))

      result shouldBe None
    }
  }

  "identifierIsUnique" should {
    "return false when individual identifiers already exist" in new GeneratedTestIndividual {
      val testUser = await(repository.createUser(testIndividual))

      await(repository.identifierIsUnique(testUser.saUtr.get)) shouldBe false
      await(repository.identifierIsUnique(testUser.nino.get)) shouldBe false
      await(repository.identifierIsUnique(testUser.vrn.get)) shouldBe false
      await(repository.identifierIsUnique(testUser.mtdItId.get)) shouldBe false
      await(repository.identifierIsUnique(testUser.eoriNumber.get)) shouldBe false
    }

    "return false when organisation identifiers already exist" in new GeneratedTestOrganisation {
      val testUser = await(repository.createUser(testOrganisation))

      await(repository.identifierIsUnique(testUser.saUtr.get)) shouldBe false
      await(repository.identifierIsUnique(testUser.nino.get)) shouldBe false
      await(repository.identifierIsUnique(testUser.vrn.get)) shouldBe false
      await(repository.identifierIsUnique(testUser.empRef.get)) shouldBe false
      await(repository.identifierIsUnique(testUser.ctUtr.get)) shouldBe false
      await(repository.identifierIsUnique(testUser.mtdItId.get)) shouldBe false
      await(repository.identifierIsUnique(testUser.lisaManRefNum.get)) shouldBe false
      await(repository.identifierIsUnique(testUser.eoriNumber.get)) shouldBe false
    }
  }
}
