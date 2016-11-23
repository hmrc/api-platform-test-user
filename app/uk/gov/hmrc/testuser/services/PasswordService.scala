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

package uk.gov.hmrc.testuser.services

import javax.inject.Inject

import org.mindrot.jbcrypt.{BCrypt => BCryptUtils}
import uk.gov.hmrc.testuser.config.AppContext

trait PasswordService {
  val logRounds: Int

  def hash(password: String): String = BCryptUtils.hashpw(password, BCryptUtils.gensalt(logRounds))

  def validate(password: String, hashedPassword: String) = BCryptUtils.checkpw(password, hashedPassword)
}

class PasswordServiceImpl @Inject()(appContext: AppContext) extends PasswordService {
  override lazy val logRounds = appContext.passwordLogRounds
}
