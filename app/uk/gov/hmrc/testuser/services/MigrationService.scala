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

package uk.gov.hmrc.testuser.services

import javax.inject.{Inject, Singleton}

import org.joda.time.Duration._
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.json.Json.{obj, toJson}
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers._
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.lock.{LockKeeper, LockMongoRepository}
import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.util.Randomiser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.sequence

@Singleton
class MigrationService @Inject()(repository: TestUserRepository, generator: Generator) extends MongoDbConnection with Randomiser {

  implicit lazy val jsonCollection = repository.collection

  private val lockKeeper = new LockKeeper {
    lazy val mongo: () => DB = mongoConnector.db

    override def repo = LockMongoRepository(mongo)

    override def lockId = "migrate-api-platform-test-user"

    override val forceLockReleaseAfter = standardMinutes(2)
  }

  private val userFullNameAndEmailAddressMigration = new Migration("user-full-name-and-email-address") {
    override protected val query = BSONDocument(
      "userFullName" -> obj("$exists" -> false)
    )

    override protected def selector(jsValue: JsValue) =
      BSONDocument("userId" -> (jsValue \ "userId").as[String])

    override protected def modifier = {
      val firstName = generator.generateFirstName
      val lastName = generator.generateLastName

      BSONDocument("$set" ->
        BSONDocument("userFullName" -> toJson(generator.generateUserFullName(firstName, lastName)),
        "emailAddress" -> toJson(generator.generateEmailAddress(firstName, lastName)))
      )
    }

  }


  def migrate(): Future[Unit] = migrate(Seq(userFullNameAndEmailAddressMigration))

  private def migrate(migrations: Seq[Migration]): Future[Unit] =
    lockKeeper.tryLock {
      sequence(migrations map (_.migrate()))
    } map {
      case None => Logger.warn(s"migrations skipped due to unobtainable database lock")
      case Some(_) =>
    }

}

private abstract class Migration(val key: String)(implicit jsonCollection: JSONCollection) {

  protected val query: BSONDocument

  protected def selector(jsValue: JsValue): BSONDocument

  protected def modifier: BSONDocument

  def migrate() =
    jsonCollection.find(query).cursor[JsValue]().collect[List]() flatMap { documents =>
      Logger.info(s"migration '$key' identified ${documents.size} documents for update")
      sequence(documents map { document =>
        jsonCollection.update(selector(document), modifier)
      })
    } map { updateWriteResults =>
      Logger.info(s"migration '$key' succeeded with ${updateWriteResults.size} documents updated")
    } recover {
      case throwable: Throwable =>
        Logger.error(s"migration '$key' failed", throwable)
    }

}
