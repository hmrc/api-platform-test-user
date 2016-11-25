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

package uk.gov.hmrc.testuser.controllers

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Result, Action}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.services._

import scala.concurrent.ExecutionContext.Implicits.global

trait TestUserController extends BaseController {

  val testUserService: TestUserService

  def createIndividual() = Action.async { implicit request =>
    testUserService.createTestIndividual() map { individual =>
      Created(Json.toJson(CreateTestIndividualResponse.from(individual)).toString())
    } recover recovery
  }

  def createOrganisation() = Action.async { implicit request =>
    testUserService.createTestOrganisation() map { organisation =>
      Created(Json.toJson(CreateTestOrganisationResponse.from(organisation)).toString())
    } recover recovery
  }

  private def getUser(username: String, password: String) =
    testUserService.testUserRepository.fetchByUsername(username).map {
      case None => Unauthorized(Json.toJson(ErrorResponse.usernameNotFoundError(username)))
      case Some(ind @ TestIndividual(`username`, hashedPass, _, _, _)) if testUserService.passwordService.validate(password, hashedPass) =>
        Ok(Json.toJson(TestIndividualResponse.from(ind)).toString())
      case Some(org @ TestOrganisation(`username`, hashedPass, _, _, _, _, _)) if testUserService.passwordService.validate(password, hashedPass) =>
        Ok(Json.toJson(TestOrganisationResponse.from(org)).toString())
      case _ => Unauthorized(Json.toJson(ErrorResponse.wrongPasswordError(username)))
    } recover recovery

  def authenticate() = Action.async(parse.json) {
    implicit request =>
      withJsonBody[AuthenticationRequest] {
        authRequest: AuthenticationRequest => getUser(authRequest.username, authRequest.password)
    } recover recovery
  }

  private def recovery: PartialFunction[Throwable, Result] = {
    case e =>
      Logger.error(s"An unexpected error occurred: ${e.getMessage}", e)
      InternalServerError(Json.toJson(ErrorResponse.internalServerError))
  }
}

class TestUserControllerImpl @Inject()(override val testUserService: TestUserServiceImpl) extends TestUserController
