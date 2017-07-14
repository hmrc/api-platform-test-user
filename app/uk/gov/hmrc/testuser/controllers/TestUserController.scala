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

package uk.gov.hmrc.testuser.controllers

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.domain.{SaUtr, Nino}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.testuser.models.ErrorResponse.userNotFoundError
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.services._

import scala.concurrent.ExecutionContext.Implicits.global

trait TestUserController extends BaseController {

  val testUserService: TestUserService

  def createIndividual() = Action.async(parse.json) { implicit request =>
    withJsonBody[CreateUserRequest] { createUserRequest =>
      testUserService.createTestIndividual(createUserRequest.serviceNames.getOrElse(Seq.empty)) map { individual =>
        Created(toJson(TestIndividualCreatedResponse.from(individual)))
      }
    }recover recovery
  }

  def createOrganisation() = Action.async(parse.json) { implicit request =>
    withJsonBody[CreateUserRequest] { createUserRequest =>
      testUserService.createTestOrganisation(createUserRequest.serviceNames.getOrElse(Seq.empty)) map { organisation =>
        Created(toJson(TestOrganisationCreatedResponse.from(organisation)))
      }
    }recover recovery
  }

  def createAgent() = Action.async(parse.json) { implicit request =>
    withJsonBody[CreateUserRequest] { createUserRequest =>
      testUserService.createTestAgent(createUserRequest.serviceNames.getOrElse(Seq.empty)) map { agent =>
        Created(toJson(TestAgentCreatedResponse.from(agent)))
      }
    } recover recovery
  }

  def fetchIndividualByNino(nino: Nino) = Action.async { implicit request =>
    testUserService.fetchIndividualByNino(nino) map { individual =>
      Ok(toJson(TestIndividualResponse.from(individual)))
    } recover recovery
  }

  def fetchIndividualByShortNino(shortNino: NinoNoSuffix) = Action.async { implicit request =>
    testUserService.fetchIndividualByShortNino(shortNino) map { individual =>
      Ok(toJson(TestIndividualResponse.from(individual)))
    } recover recovery
  }

  def fetchIndividualBySaUtr(saUtr: SaUtr) = Action.async { implicit request =>
    testUserService.fetchIndividualBySaUtr(saUtr) map { individual =>
      Ok(toJson(TestIndividualResponse.from(individual)))
    } recover recovery
  }

  private def recovery: PartialFunction[Throwable, Result] = {
    case UserNotFound() => NotFound(toJson(userNotFoundError))
    case e =>
      Logger.error(s"An unexpected error occurred: ${e.getMessage}", e)
      InternalServerError(toJson(ErrorResponse.internalServerError))
  }
}

class TestUserControllerImpl @Inject()(override val testUserService: TestUserServiceImpl) extends TestUserController

