/*
 * Copyright 2022 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.domain._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.testuser.models._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestUserRepository @Inject() (mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext)
    extends ReactiveRepository[TestUser, BSONObjectID]("testUser", mongo.mongoConnector.db, JsonFormatters.formatTestUser, ReactiveMongoFormats.objectIdFormats) {

  // List of fields that contain generated identifiers
  val IdentifierFields: Seq[String] = Seq("nino", "saUtr", "vrn", "empRef", "mtdItId", "ctUtr", "lisaManRefNum", "eoriNumber", "arn", "groupIdentifier", "crn")

  ensureIndex("userId", "userIdIndex")

  // Create indexes for ech identifier field - need to be non-unique as we may have existing duplicate values
  IdentifierFields.foreach(identifierField =>
    ensureIndex(identifierField, s"$identifierField-Index", isUnique = false)
  )

  def createUser[T <: TestUser](testUser: T): Future[T] = {
    insert(testUser) map { _ => testUser }
  }

  def fetchByUserId(userId: String): Future[Option[TestUser]] = {
    find("userId" -> userId) map (_.headOption)
  }

  private def ensureIndex(field: String, indexName: String, isUnique: Boolean = true): Future[Boolean] = {
    collection.indexesManager
      .ensure(Index(Seq(field -> IndexType.Ascending), name = Some(indexName), unique = isUnique, background = true))
  }

  def fetchIndividualByNino(nino: Nino): Future[Option[TestIndividual]] = {
    find("nino" -> nino, "userType" -> UserType.INDIVIDUAL) map (_.headOption map (_.asInstanceOf[TestIndividual]))
  }

  def fetchByNino(nino: Nino): Future[Option[TestUser]] = {
    find("nino" -> nino) map (_.headOption map (_.asInstanceOf[TestUser]))
  }

  def fetchIndividualByShortNino(shortNino: NinoNoSuffix): Future[Option[TestIndividual]] = {
    val matchShortNino = Json.obj("$regex" -> s"${shortNino.value}\\w")
    find("nino" -> matchShortNino, "userType" -> UserType.INDIVIDUAL) map (_.headOption map (_.asInstanceOf[TestIndividual]))
  }

  def fetchIndividualBySaUtr(saUtr: SaUtr): Future[Option[TestIndividual]] = {
    find("saUtr" -> saUtr, "userType" -> UserType.INDIVIDUAL) map (_.headOption map (_.asInstanceOf[TestIndividual]))
  }

  def fetchIndividualByVrn(vrn: Vrn): Future[Option[TestIndividual]] = {
    find("vrn" -> vrn, "userType" -> UserType.INDIVIDUAL) map (_.headOption map (_.asInstanceOf[TestIndividual]))
  }

  def fetchOrganisationByEmpRef(empRef: EmpRef): Future[Option[TestOrganisation]] = {
    find("empRef" -> empRef.value) map (_.headOption map (_.asInstanceOf[TestOrganisation]))
  }

  def fetchOrganisationByCtUtr(utr: CtUtr): Future[Option[TestOrganisation]] = {
    find("ctUtr" -> utr.value) map (_.headOption map (_.asInstanceOf[TestOrganisation]))
  }

  def fetchOrganisationByVrn(vrn: Vrn): Future[Option[TestOrganisation]] = {
    find("vrn" -> vrn.value, "userType" -> UserType.ORGANISATION) map (_.headOption map (_.asInstanceOf[TestOrganisation]))
  }

  def fetchOrganisationBySaUtr(saUtr: SaUtr): Future[Option[TestOrganisation]] = {
    find("saUtr" -> saUtr, "userType" -> UserType.ORGANISATION) map (_.headOption map (_.asInstanceOf[TestOrganisation]))
  }

  def fetchOrganisationByCrn(crn: Crn): Future[Option[TestOrganisation]] = {
    find("crn" -> crn.value, "userType" -> UserType.ORGANISATION) map (_.headOption map (_.asInstanceOf[TestOrganisation]))
  }

  def identifierIsUnique(identifier: String): Future[Boolean] = {
    logger.info(s"Checking tax identifier uniqueness - $identifier")
    val query = Json.obj("$or" -> IdentifierFields.map(identifierField => Json.obj(identifierField -> identifier)))
    count(query).map { matchedIdentifiers =>
      val isUnique = matchedIdentifiers == 0
      logger.info(s"Completed checking tax identifier uniqueness - $identifier")
      isUnique
    }
  }
}
