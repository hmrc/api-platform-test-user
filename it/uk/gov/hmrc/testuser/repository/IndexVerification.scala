/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest.concurrent.Eventually
import org.mongodb.scala.model.IndexModel
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.testuser.common.utils.AsyncHmrcSpec

trait IndexVerification extends AsyncHmrcSpec with Eventually {

  def verifyIndex[A](repository: PlayMongoRepository[A], expectedIndex: Map[String, Any]) = {
      val actualIndexes = await(repository.collection.listIndexes[Seq[IndexModel]]().toFuture())
      assert(actualIndexes.contains(expectedIndex))
  }
}

