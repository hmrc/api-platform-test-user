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

package uk.gov.hmrc.testuser.controllers

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.json.Json._
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models.{AuthenticationRequest, AuthenticationResponse, ErrorResponse, InvalidCredentials}
import uk.gov.hmrc.testuser.services.AuthenticationService

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AuthenticationController @Inject()(val authenticationService: AuthenticationService) extends BaseController {

  def authenticate() = {
    Action.async(parse.json) { implicit request =>
      withJsonBody[AuthenticationRequest] {
        authenticationService.authenticate(_) map { case (testUser, authSession) =>
          Created(toJson(AuthenticationResponse(authSession.gatewayToken, testUser.affinityGroup))).withHeaders(
            HeaderNames.AUTHORIZATION -> authSession.authBearerToken,
            HeaderNames.LOCATION -> authSession.authorityUri)
        }
      } recover {
        case _: InvalidCredentials => Unauthorized(toJson(ErrorResponse.invalidCredentialsError))
      } recover recovery
    }
  }

  private def recovery: PartialFunction[Throwable, Result] = {
    case e =>
      Logger.error(s"An unexpected error occurred: ${e.getMessage}", e)
      InternalServerError(toJson(ErrorResponse.internalServerError))
  }
}
