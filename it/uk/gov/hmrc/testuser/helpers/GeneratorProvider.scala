package uk.gov.hmrc.testuser.helpers

import com.typesafe.config.ConfigFactory
import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.services.Generator

import scala.concurrent.ExecutionContext

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
      |""".stripMargin)

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def repository: TestUserRepository

  def generator: Generator = new Generator(repository, config)
}
