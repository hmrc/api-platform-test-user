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

package uk.gov.hmrc.testuser.docs

import com.eclipsesource.schema._
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsResult, JsValue, Json, Reads}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.JsonFormatters._

import scala.io.Source

class ExampleSpec extends UnitSpec with MockitoSugar {

  "example create agent request" should {

    val filename = "create-agent-request"

    "deserialize into an object" in {
      testDeserialization[CreateUserRequest](filename)
    }

    "be valid against the JSON schema" in {
      testSchemaValidation(filename)
    }
  }

  "example create agent response" should {

    val filename = "create-agent-response"

    "deserialize into an object" in {
      testDeserialization[TestAgentCreatedResponse](filename)
    }

    "be valid against the JSON schema" in {
      testSchemaValidation(filename)
    }
  }

  "example create individual request" should {

    val filename = "create-individual-request"

    "deserialize into an object" in {
      testDeserialization[CreateUserRequest](filename)
    }

    "be valid against the JSON schema" in {
      testSchemaValidation(filename)
    }
  }

  "example create individual response" should {

    val filename = "create-individual-response"

    "deserialize into an object" in {
      testDeserialization[TestIndividualCreatedResponse](filename)
    }

    "be valid against the JSON schema" in {
      testSchemaValidation(filename)
    }
  }

  "example create organisation request" should {

    val filename = "create-organisation-request"

    "deserialize into an object" in {
      testDeserialization[CreateUserRequest](filename)
    }

    "be valid against the JSON schema" in {
      testSchemaValidation(filename)
    }
  }

  "example create organisation response" should {

    val filename = "create-organisation-response"

    "deserialize into an object" in {
      testDeserialization[TestOrganisationCreatedResponse](filename)
    }

    "be valid against the JSON schema" in {
      testSchemaValidation(filename)
    }
  }

  private def testDeserialization[A](filename: String)(
      implicit fjs: Reads[A]) = {
    val examplePath = generateExamplePath(filename)
    safelyReadFileContents(examplePath) { exampleContents =>
      val deserialized = Json.fromJson[A](Json.parse(exampleContents))
      deserialized.asOpt shouldNot be(None)
    }
  }

  private def testSchemaValidation(filename: String) = {
    val examplePath = generateExamplePath(filename)
    val schemaPath = generateSchemaPath(filename)
    safelyReadFileContents(schemaPath) { schemaContents =>
      safelyReadFileContents(examplePath) { exampleContents =>
        val schema: SchemaType =
          Json.fromJson[SchemaType](Json.parse(schemaContents)).get
        val validator = SchemaValidator().validate(schema)(_)
        val result: JsResult[JsValue] = validator(Json.parse(exampleContents))
        result.isSuccess shouldBe true
      }
    }
  }

  val resourcesPathV10 = "./resources/public/api/conf/1.0/"

  private def generateExamplePath(filename: String) =
    resourcesPathV10 + "examples/" + filename + ".json"

  private def generateSchemaPath(filename: String) =
    resourcesPathV10 + "schemas/" + filename + ".json"

  private def safelyReadFileContents[A](path: String)(f: String => A): A = {
    val bufferedSource = Source.fromFile(path)
    try {
      f(bufferedSource.getLines().mkString)
    } finally {
      bufferedSource.close()
    }
  }

}
