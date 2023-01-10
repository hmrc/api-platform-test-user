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

package uk.gov.hmrc.testuser.repository

import org.mongodb.scala.model.Filters
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import uk.gov.hmrc.domain._
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.testuser.helpers.GeneratorProvider
import uk.gov.hmrc.testuser.models.ServiceKeys._
import uk.gov.hmrc.testuser.models.{Crn, NinoNoSuffix}

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.testuser.common.utils.AsyncHmrcSpec

class TestUserRepositorySpec extends AsyncHmrcSpec with BeforeAndAfterEach with BeforeAndAfterAll with MongoSupport with IndexVerification {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val userRepository                = new TestUserRepository(mongoComponent)

  trait GeneratedTestIndividual extends GeneratorProvider {
    val repository = userRepository

    val testIndividual = await(generator.generateTestIndividual(Seq(MTD_INCOME_TAX, SELF_ASSESSMENT, NATIONAL_INSURANCE, MTD_VAT, CUSTOMS_SERVICES, CTC_LEGACY, CTC), None, None))
  }

  trait GeneratedTestOrganisation extends GeneratorProvider {
    val repository = userRepository

    val testOrganisation =
      await(
        generator.generateTestOrganisation(
          Seq(MTD_INCOME_TAX, SELF_ASSESSMENT, NATIONAL_INSURANCE, CORPORATION_TAX, PAYE_FOR_EMPLOYERS, MTD_VAT, LISA, CUSTOMS_SERVICES, CTC_LEGACY, CTC),
          eoriNumber = None,
          nino = None,
          taxpayerType = None
        )
      )
  }

  override def beforeEach: Unit = {
    await(userRepository.collection.drop.toFuture())
    await(userRepository.ensureIndexes)
  }

  "indexes" should {
    "be created for userId" in {
      val expectedIndex =
        Seq(BsonDocument("name" -> "userIdIndex", "unique" -> true, "background" -> true, "key" -> BsonDocument("userId" -> 1)))
      verifyIndexes(userRepository, expectedIndex)
    }

    "be created for all identifier fields" in {
      def expectedIndexes: Seq[BsonDocument] =
        userRepository.IdentifierFields
          .map(identifierField => BsonDocument("name" -> s"$identifierField-Index", "background" -> true, "key" -> BsonDocument(identifierField -> 1)))
          .toSeq

      verifyIndexes(userRepository, expectedIndexes)
    }
  }

  "createUser" should {

    "create a test individual in the repository" in new GeneratedTestIndividual {
      val result = await(repository.createUser(testIndividual))

      result shouldBe testIndividual
      await(repository.collection.find(Filters.equal("_id", testIndividual._id)).headOption) shouldBe Some(testIndividual)
    }

    "create a test organisation in the repository" in new GeneratedTestOrganisation {
      val result = await(repository.createUser(testOrganisation))

      result shouldBe testOrganisation
      await(repository.collection.find(Filters.equal("_id", testOrganisation._id)).headOption) shouldBe Some(testOrganisation)
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

      val result = await(userRepository.fetchByUserId("unknown"))

      result shouldBe None
    }
  }

  "fetchIndividualByNino" should {

    "return the individual" in new GeneratedTestIndividual {
      await(repository.createUser(testIndividual))

      val result = await(repository.fetchIndividualByNino(Nino(testIndividual.nino.get)))

      result shouldBe Some(testIndividual)
    }

    "return None when there is no individual matching" in {
      val result = await(userRepository.fetchIndividualByNino(Nino("CC333334C")))

      result shouldBe None
    }

    "return None when there is an organisation matching" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchIndividualByNino(Nino(testOrganisation.nino.get)))

      result shouldBe None
    }
  }

  "fetchIndividualByShortNino" should {
    val nino             = Nino("CC333333C")
    val validShortNino   = NinoNoSuffix("CC333333")
    val invalidShortNino = NinoNoSuffix("CC333334")

    "return the individual" in new GeneratedTestIndividual {
      val individual = testIndividual.copy(nino = Some(nino.toString()))
      await(repository.createUser(individual))

      val result = await(repository.fetchIndividualByShortNino(validShortNino))

      result shouldBe Some(individual)
    }

    "return None when there is no individual matching" in {
      val result = await(userRepository.fetchIndividualByShortNino(invalidShortNino))

      result shouldBe None
    }

    "return None when there is an organisation matching" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchIndividualByShortNino(NinoNoSuffix(testOrganisation.nino.get.substring(0, 8))))

      result shouldBe None
    }
  }

  "fetchByNino" should {
    val nino        = Nino("CC333333C")
    val invalidNino = Nino("CC333334C")

    "return the user" in new GeneratedTestIndividual {
      val individual = testIndividual.copy(nino = Some(nino.toString()))
      await(repository.createUser(individual))

      val result = await(repository.fetchByNino(nino))

      result shouldBe Some(individual)
    }

    "return the organisation" in new GeneratedTestOrganisation {
      val organisation = testOrganisation.copy(nino = Some(nino.toString()))
      await(repository.createUser(organisation))

      val result = await(repository.fetchByNino(nino))

      result shouldBe Some(organisation)
    }

    "return None when there is no individual matching" in {
      val result = await(userRepository.fetchByNino(invalidNino))

      result shouldBe None
    }
  }

  "fetchIndividualBySaUtr" should {

    "return the individual" in new GeneratedTestIndividual {
      await(repository.createUser(testIndividual))

      val result = await(repository.fetchIndividualBySaUtr(SaUtr(testIndividual.saUtr.get)))

      result shouldBe Some(testIndividual)
    }

    "return None when there is an organisation matching" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchIndividualBySaUtr(SaUtr(testOrganisation.saUtr.get)))

      result shouldBe None
    }

    "return None when there is no individual matching" in {
      val result = await(userRepository.fetchIndividualBySaUtr(SaUtr("1555369052")))

      result shouldBe None
    }
  }

  "fetchIndividualByVrn" should {

    "return the individual" in new GeneratedTestIndividual {
      await(repository.createUser(testIndividual))

      val result = await(repository.fetchIndividualByVrn(Vrn(testIndividual.vrn.get)))

      result shouldBe Some(testIndividual)
    }

    "return None when there is an organisation matching" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchIndividualByVrn(Vrn(testOrganisation.vrn.get)))

      result shouldBe None
    }

    "return None when there is no individual matching" in {
      val result = await(userRepository.fetchIndividualByVrn(Vrn("1555369052")))

      result shouldBe None
    }
  }

  "fetchOrganisationByEmpRef" should {

    "return the organisation" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(testOrganisation.empRef.get)))

      result shouldBe Some(testOrganisation)
    }

    "return None when there is an organisation matching" in new GeneratedTestOrganisation {
      val result = await(repository.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(testOrganisation.empRef.get)))

      result shouldBe None
    }
  }

  "fetchOrganisationByVrn" should {

    "return the organisation" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchOrganisationByVrn(Vrn(testOrganisation.vrn.get)))

      result shouldBe Some(testOrganisation)
    }

    "return None when there is an individual matching" in new GeneratedTestIndividual {
      await(repository.createUser(testIndividual))

      val result = await(repository.fetchOrganisationByVrn(Vrn(testIndividual.vrn.get)))

      result shouldBe None
    }

    "return None when there is an organisation matching" in new GeneratedTestOrganisation {
      val result = await(repository.fetchOrganisationByVrn(Vrn(testOrganisation.vrn.get)))

      result shouldBe None
    }
  }

  "fetchOrganisationByCtUtr" should {

    "return the organisation" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchOrganisationByCtUtr(CtUtr(testOrganisation.ctUtr.get)))

      result shouldBe Some(testOrganisation)
    }

    "return None when there is no organisation matching" in new GeneratedTestOrganisation {
      val result = await(repository.fetchOrganisationByCtUtr(CtUtr(testOrganisation.ctUtr.get)))
      result shouldBe None
    }
  }

  "fetchOrganisationBySaUtr" should {

    "return the organisation" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchOrganisationBySaUtr(SaUtr(testOrganisation.saUtr.get)))

      result shouldBe Some(testOrganisation)
    }

    "return None when there is no organisation matching" in new GeneratedTestOrganisation {
      val result = await(repository.fetchOrganisationBySaUtr(SaUtr(testOrganisation.saUtr.get)))
      result shouldBe None
    }
  }

  "fetchOrganisationByCrn" should {

    "return the organisation" in new GeneratedTestOrganisation {
      await(repository.createUser(testOrganisation))

      val result = await(repository.fetchOrganisationByCrn(Crn(testOrganisation.crn.get)))

      result shouldBe Some(testOrganisation)
    }

    "return None when there is no organisation matching" in new GeneratedTestOrganisation {
      val result = await(repository.fetchOrganisationByCrn(Crn(testOrganisation.crn.get)))
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
