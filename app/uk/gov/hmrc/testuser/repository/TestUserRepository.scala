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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}

import play.api.libs.json.Json
import uk.gov.hmrc.domain._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import uk.gov.hmrc.testuser.models._

@Singleton
class TestUserRepository @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext)
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
        ),
        IndexModel(
          ascending("saUtr"),
          IndexOptions()
            .name("saUtr-Index")
            .background(true)
        ),
        IndexModel(
          ascending("vrn"),
          IndexOptions()
            .name("vrn-Index")
            .background(true)
        ),
        IndexModel(
          ascending("empRef"),
          IndexOptions()
            .name("empRef-Index")
            .background(true)
        ),
        IndexModel(
          ascending("mtdItId"),
          IndexOptions()
            .name("mtdItId-Index")
            .background(true)
        ),
        IndexModel(
          ascending("ctUtr"),
          IndexOptions()
            .name("ctUtr-Index")
            .background(true)
        ),
        IndexModel(
          ascending("lisaManRefNum"),
          IndexOptions()
            .name("lisaManRefNum-Index")
            .background(true)
        ),
        IndexModel(
          ascending("eoriNumber"),
          IndexOptions()
            .name("eoriNumber-Index")
            .background(true)
        ),
        IndexModel(
          ascending("arn"),
          IndexOptions()
            .name("arn-Index")
            .background(true)
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
        ),
        IndexModel(
          ascending("exciseNumber"),
          IndexOptions()
            .name("exciseNumber-Index")
            .background(true)
            .sparse(true)
        )
      ),
      replaceIndexes = true
    ) {

  // List of fields that contain generated identifiers
  val IdentifierFields: Seq[String] = Seq("nino", "saUtr", "vrn", "empRef", "mtdItId", "ctUtr", "lisaManRefNum", "eoriNumber", "arn", "groupIdentifier", "crn")

  def createUser[T <: TestUser](testUser: T): Future[T] = {
    collection.insertOne(testUser)
      .toFuture()
      .map(_ => testUser)
  }

  def fetchByUserId(userId: String): Future[Option[TestUser]] = {
    collection.find(equal("userId", userId)).headOption()
  }

  def fetchIndividualByNino(nino: Nino): Future[Option[TestIndividual]] = {
    collection.find(
      and(
        equal("nino", Codecs.toBson(nino)),
        equal("userType", UserType.INDIVIDUAL.toString)
      )
    ).toFuture() map (_.headOption map (_.asInstanceOf[TestIndividual]))
  }

  def fetchByNino(nino: Nino): Future[Option[TestUser]] = {
    collection.find(
      equal("nino", Codecs.toBson(nino))
    ).toFuture() map (_.headOption map (_.asInstanceOf[TestUser]))
  }

  def fetchIndividualByShortNino(shortNino: NinoNoSuffix): Future[Option[TestIndividual]] = {
    val matchShortNino = Json.obj("$regex" -> s"${shortNino.value}\\w")
    collection.find(
      and(
        equal("nino", Codecs.toBson(matchShortNino)),
        equal("userType", UserType.INDIVIDUAL.toString)
      )
    ).toFuture() map (_.headOption map (_.asInstanceOf[TestIndividual]))
  }

  def fetchIndividualBySaUtr(saUtr: SaUtr): Future[Option[TestIndividual]] = {
    collection.find(
      and(
        equal("saUtr", Codecs.toBson(saUtr)),
        equal("userType", UserType.INDIVIDUAL.toString())
      )
    ).toFuture() map (_.headOption map (_.asInstanceOf[TestIndividual]))
  }

  def fetchIndividualByVrn(vrn: Vrn): Future[Option[TestIndividual]] = {
    collection.find(
      and(
        equal("vrn", Codecs.toBson(vrn)),
        equal("userType", UserType.INDIVIDUAL.toString())
      )
    ).toFuture() map (_.headOption map (_.asInstanceOf[TestIndividual]))
  }

  def fetchOrganisationByEmpRef(empRef: EmpRef): Future[Option[TestOrganisation]] = {
    collection.find(
      equal("empRef", Codecs.toBson(empRef))
    ).toFuture() map (_.headOption map (_.asInstanceOf[TestOrganisation]))
  }

  def fetchOrganisationByCtUtr(utr: CtUtr): Future[Option[TestOrganisation]] = {
    collection.find(
      equal("ctUtr", Codecs.toBson(utr))
    ).toFuture() map (_.headOption map (_.asInstanceOf[TestOrganisation]))
  }

  def fetchOrganisationByVrn(vrn: Vrn): Future[Option[TestOrganisation]] = {
    collection.find(
      and(
        equal("vrn", Codecs.toBson(vrn)),
        equal("userType", UserType.ORGANISATION.toString)
      )
    ).toFuture() map (_.headOption map (_.asInstanceOf[TestOrganisation]))
  }

  def fetchOrganisationBySaUtr(saUtr: SaUtr): Future[Option[TestOrganisation]] = {
    collection.find(
      and(
        equal("saUtr", Codecs.toBson(saUtr)),
        equal("userType", UserType.ORGANISATION.toString)
      )
    ).toFuture() map (_.headOption map (_.asInstanceOf[TestOrganisation]))
  }

  def fetchOrganisationByCrn(crn: Crn): Future[Option[TestOrganisation]] = {
    collection.find(
      and(
        equal("crn", crn.value),
        equal("userType", UserType.ORGANISATION.toString)
      )
    ).toFuture() map (_.headOption map (_.asInstanceOf[TestOrganisation]))
  }

  def identifierIsUnique(propKey: TestUserPropKey)(identifier: String): Future[Boolean] = {
    collection.find(equal(propKey.toString, identifier)).limit(1).headOption().map(_.isEmpty)
  }
}
