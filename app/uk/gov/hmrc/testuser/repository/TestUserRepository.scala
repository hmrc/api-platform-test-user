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

package uk.gov.hmrc.testuser.repository

import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}
import uk.gov.hmrc.testuser.models.{JsonFormatters, TestUser}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

trait TestUserRepository extends Repository[TestUser, BSONObjectID] {

  def createUser[T <: TestUser](testUser:T): Future[T]

  def fetchByUserId(userId: String): Future[Option[TestUser]]
}

class TestUserMongoRepository(implicit mongo: () => DB)
  extends ReactiveRepository[TestUser, BSONObjectID]("testUser", mongo,
    JsonFormatters.formatTestUser, ReactiveMongoFormats.objectIdFormats)
  with TestUserRepository {

  ensureIndex("userId", "userIdIndex")

  override def createUser[T <: TestUser](testUser: T): Future[T] = {
    insert(testUser) map {_ => testUser}
  }

  override def fetchByUserId(userId: String): Future[Option[TestUser]] = {
    find("userId" -> userId) map(_.headOption)
  }

  private def ensureIndex(field: String, indexName: String, isUnique: Boolean = true): Future[Boolean] = {
    collection.indexesManager.ensure(Index(Seq(field -> IndexType.Ascending),
      name = Some(indexName), unique = isUnique, background = true))
  }
}

object TestUserRepository extends MongoDbConnection {

  private lazy val repository = new TestUserMongoRepository

  def apply(): TestUserRepository = repository
}
