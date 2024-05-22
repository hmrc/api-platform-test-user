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

import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import org.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Aggregates, Field, IndexModel, IndexOptions}

import play.api.libs.json.Json
import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow
import uk.gov.hmrc.domain._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import uk.gov.hmrc.testuser.models._

@Singleton
class TestUserRepository @Inject() (mongo: MongoComponent, val clock: Clock)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[TestUser](
      collectionName = "testUser",
      mongoComponent = mongo,
      domainFormat = JsonFormatters.formatTestUser,
      extraCodecs = Codecs.playFormatSumCodecs(JsonFormatters.formatTestUser),
      indexes = Seq(
        IndexModel(
          ascending("userId"),
          IndexOptions()
            .name("userIdIndex")
            .unique(true)
            .background(true)
        ),
        IndexModel(
          ascending("nino"),
          IndexOptions()
            .name("nino-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("saUtr"),
          IndexOptions()
            .name("saUtr-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("vrn"),
          IndexOptions()
            .name("vrn-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("empRef"),
          IndexOptions()
            .name("empRef-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("mtdItId"),
          IndexOptions()
            .name("mtdItId-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("ctUtr"),
          IndexOptions()
            .name("ctUtr-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("lisaManRefNum"),
          IndexOptions()
            .name("lisaManRefNum-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("eoriNumber"),
          IndexOptions()
            .name("eoriNumber-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("arn"),
          IndexOptions()
            .name("arn-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("groupIdentifier"),
          IndexOptions()
            .name("groupIdentifier-Index")
            .background(true)
        ),
        IndexModel(
          ascending("crn"),
          IndexOptions()
            .name("crn-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("exciseNumber"),
          IndexOptions()
            .name("exciseNumber-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("secureElectronicTransferReferenceNumber"),
          IndexOptions()
            .name("setRefNbr-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("pensionSchemeAdministratorIdentifier"),
          IndexOptions()
            .name("pensionSchemeAdminId-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("taxpayerType"),
          IndexOptions()
            .name("taxpayerType-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("agentCode"),
          IndexOptions()
            .name("agentCode-Index")
            .background(true)
            .sparse(true)
        ),
        IndexModel(
          ascending("lastAccess"),
          IndexOptions()
            .name("lastAccess-Index")
            .background(true)
            .sparse(true)
        )
      ),
      replaceIndexes = true
    ) with ClockNow {

  def createUser[T <: TestUser](testUser: T): Future[T] = {
    collection.insertOne(testUser)
      .toFuture()
      .flatMap { inserted =>
        val byId = equal("_id", inserted.getInsertedId())
        fetchMarkAccess(byId)
      }
      .map(_ => testUser)
  }

  private def fetchMarkAccess(query: Bson): Future[Option[TestUser]] = {
    val aggregates = Seq(Aggregates.set(Field("lastAccess", now())))
    collection.findOneAndUpdate(query, aggregates).headOption()
  }

  private def fetchMarkAccessAs[T <: TestUser](query: Bson): Future[Option[T]] = {
    fetchMarkAccess(query) map (_ map (_.asInstanceOf[T]))
  }

  def fetchByUserId(userId: String): Future[Option[TestUser]] = {
    fetchMarkAccess(equal("userId", userId))
  }

  def fetchIndividualByNino(nino: Nino): Future[Option[TestIndividual]] = {
    fetchMarkAccessAs[TestIndividual](
      and(
        equal("nino", Codecs.toBson(nino)),
        equal("userType", UserType.INDIVIDUAL.toString)
      )
    )
  }

  def fetchByNino(nino: Nino): Future[Option[TestUser]] = {
    fetchMarkAccessAs[TestUser](
      equal("nino", Codecs.toBson(nino))
    )
  }

  def fetchIndividualByShortNino(shortNino: NinoNoSuffix): Future[Option[TestIndividual]] = {
    val matchShortNino = Json.obj("$regex" -> s"${shortNino.value}\\w")
    fetchMarkAccessAs[TestIndividual](
      and(
        equal("nino", Codecs.toBson(matchShortNino)),
        equal("userType", UserType.INDIVIDUAL.toString)
      )
    )
  }

  def fetchIndividualBySaUtr(saUtr: SaUtr): Future[Option[TestIndividual]] = {
    fetchMarkAccessAs[TestIndividual](
      and(
        equal("saUtr", Codecs.toBson(saUtr)),
        equal("userType", UserType.INDIVIDUAL.toString())
      )
    )
  }

  def fetchIndividualByVrn(vrn: Vrn): Future[Option[TestIndividual]] = {
    fetchMarkAccessAs[TestIndividual](
      and(
        equal("vrn", Codecs.toBson(vrn)),
        equal("userType", UserType.INDIVIDUAL.toString())
      )
    )
  }

  def fetchOrganisationByEmpRef(empRef: EmpRef): Future[Option[TestOrganisation]] = {
    fetchMarkAccessAs[TestOrganisation](
      equal("empRef", Codecs.toBson(empRef))
    )
  }

  def fetchOrganisationByCtUtr(utr: CtUtr): Future[Option[TestOrganisation]] = {
    fetchMarkAccessAs[TestOrganisation](
      equal("ctUtr", Codecs.toBson(utr))
    )
  }

  def fetchOrganisationByVrn(vrn: Vrn): Future[Option[TestOrganisation]] = {
    fetchMarkAccessAs[TestOrganisation](
      and(
        equal("vrn", Codecs.toBson(vrn)),
        equal("userType", UserType.ORGANISATION.toString)
      )
    )
  }

  def fetchOrganisationBySaUtr(saUtr: SaUtr): Future[Option[TestOrganisation]] = {
    fetchMarkAccessAs[TestOrganisation](
      and(
        equal("saUtr", Codecs.toBson(saUtr)),
        equal("userType", UserType.ORGANISATION.toString)
      )
    )
  }

  def fetchOrganisationByCrn(crn: Crn): Future[Option[TestOrganisation]] = {
    fetchMarkAccessAs[TestOrganisation](
      and(
        equal("crn", crn.value),
        equal("userType", UserType.ORGANISATION.toString)
      )
    )
  }

  def identifierIsUnique(propKey: TestUserPropKey)(identifier: String): Future[Boolean] = {
    collection.find(equal(propKey.toString, identifier)).limit(1).headOption().map(_.isEmpty)
  }
}
