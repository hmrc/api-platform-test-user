/*
 * Copyright 2016 HM Revenue & Customs
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

package unit.uk.gov.hmrc.testuser.services

import org.mockito.Mockito.{when, verify}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.Matchers
import org.mockito.BDDMockito.given
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.models.{TestOrganisation, TestUser, TestIndividual}
import uk.gov.hmrc.testuser.repository.TestUserRepository
import uk.gov.hmrc.testuser.services.{PasswordService, Generator, TestUserService}
import scala.concurrent.Future
import scala.concurrent.Future.successful

class TestUserServiceSpec extends UnitSpec with MockitoSugar {

  val testIndividual = TestIndividual("user", "password", SaUtr("1555369052"), Nino("CC333333C"))
  val testOrganisation = TestOrganisation("user", "password", SaUtr("1555369052"), EmpRef("555","EIA000"),
    CtUtr("1555369053"), Vrn("999902541"))

  trait Setup {
    val underTest = new TestUserService {
      override val generator: Generator = mock[Generator]
      override val testUserRepository: TestUserRepository = mock[TestUserRepository]
      override val passwordService: PasswordService = mock[PasswordService]
    }
    when(underTest.testUserRepository.createUser(Matchers.any[TestUser]())).thenAnswer(sameUser)
  }

  "createTestIndividual" should {

    "Generate an individual and save it with hashed password in the database" in new Setup {

      val hashedPassword  = "hashedPassword"
      given(underTest.generator.generateTestIndividual()).willReturn(testIndividual)
      given(underTest.passwordService.hash(testIndividual.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestIndividual())

      result shouldBe testIndividual
      verify(underTest.testUserRepository).createUser(testIndividual.copy(password = hashedPassword))
    }

    "fail when the repository fails" in new Setup {

      given(underTest.generator.generateTestIndividual()).willReturn(testIndividual)
      given(underTest.testUserRepository.createUser(Matchers.any[TestUser]())).willReturn(Future.failed(new RuntimeException()))

      intercept[RuntimeException]{await(underTest.createTestIndividual())}
    }
  }

  "createTestOrganisation" should {

    "Generate an organisation and save it in the database" in new Setup {

      val hashedPassword  = "hashedPassword"
      given(underTest.generator.generateTestOrganisation()).willReturn(testOrganisation)
      given(underTest.passwordService.hash(testOrganisation.password)).willReturn(hashedPassword)

      val result = await(underTest.createTestOrganisation())

      result shouldBe testOrganisation
      verify(underTest.testUserRepository).createUser(testOrganisation.copy(password = hashedPassword))
    }

    "fail when the repository fails" in new Setup {

      given(underTest.generator.generateTestOrganisation()).willReturn(testOrganisation)
      given(underTest.testUserRepository.createUser(Matchers.any[TestUser]())).willReturn(Future.failed(new RuntimeException()))

      intercept[RuntimeException]{await(underTest.createTestIndividual())}
    }
  }

  val sameUser = new Answer[Future[TestUser]] {
    override def answer(invocationOnMock: InvocationOnMock): Future[TestUser] = {
      successful(invocationOnMock.getArguments.head.asInstanceOf[TestUser])
    }
  }
}
