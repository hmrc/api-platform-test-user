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

package uk.gov.hmrc.testuser.helpers

import scala.concurrent.ExecutionContext

import com.typesafe.config.ConfigFactory

import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.services.Generator

trait GeneratorProvider {

  val config = ConfigFactory.parseString(
    """randomiser {
      |  individualDetails {
      |    firstName = [
      |      "Adrian"
      |    ]
      |
      |    lastName = [
      |      "Adams"
      |    ]
      |
      |    dateOfBirth = [
      |      "1940-10-10"
      |    ]
      |  }
      |
      |  address {
      |    line1 = [
      |      "1 Abbey Road"
      |    ]
      |
      |    line2 = [
      |      "Aberdeen"
      |    ]
      |
      |    postcode = [
      |      "TS1 1PA"
      |    ]
      |  }
      |}
      |""".stripMargin
  )

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def repository: TestUserRepository

  def generator: Generator = new Generator(repository, config)
}
