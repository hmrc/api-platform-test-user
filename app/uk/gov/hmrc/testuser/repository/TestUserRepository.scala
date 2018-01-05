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

package uk.gov.hmrc.testuser.repository

import javax.inject.{Inject, Singleton}

import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.domain.{EmpRef, Nino, SaUtr}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.testuser.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TestUserRepository @Inject()(mongo: ReactiveMongoComponent)
  extends ReactiveRepository[TestUser, BSONObjectID]("testUser", mongo.mongoConnector.db,
    JsonFormatters.formatTestUser, ReactiveMongoFormats.objectIdFormats) {

  ensureIndex("userId", "userIdIndex")

  def createUser[T <: TestUser](testUser: T): Future[T] = {
    insert(testUser) map {_ => testUser}
  }

  def fetchByUserId(userId: String): Future[Option[TestUser]] = {
    find("userId" -> userId) map(_.headOption)
  }

  private def ensureIndex(field: String, indexName: String, isUnique: Boolean = true): Future[Boolean] = {
    collection.indexesManager.ensure(Index(Seq(field -> IndexType.Ascending),
      name = Some(indexName), unique = isUnique, background = true))
  }

  def fetchIndividualByNino(nino: Nino): Future[Option[TestIndividual]] = {
    find("nino" -> nino, "userType" -> UserType.INDIVIDUAL) map(_.headOption map (_.asInstanceOf[TestIndividual]))
  }

  def fetchIndividualByShortNino(shortNino: NinoNoSuffix): Future[Option[TestIndividual]] = {
    val matchShortNino = Json.obj("$regex" ->  s"${shortNino.value}\\w")
    find("nino" -> matchShortNino, "userType" -> UserType.INDIVIDUAL) map(_.headOption map (_.asInstanceOf[TestIndividual]))
  }

  def fetchIndividualBySaUtr(saUtr: SaUtr): Future[Option[TestIndividual]] = {
    find("saUtr" -> saUtr, "userType" -> UserType.INDIVIDUAL) map(_.headOption map (_.asInstanceOf[TestIndividual]))
  }

  def fetchOrganisationByEmpRef(empRef: EmpRef): Future[Option[TestOrganisation]] = {
    find("empRef" -> empRef.value) map(_.headOption map (_.asInstanceOf[TestOrganisation]))
  }
}
