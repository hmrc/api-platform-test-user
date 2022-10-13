/*
 * Copyright 2022 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import org.mindrot.jbcrypt.{BCrypt => BCryptUtils}

@Singleton
class PasswordService @Inject()(config: PasswordConfig) {

  def hash(password: String): String = BCryptUtils.hashpw(password, BCryptUtils.gensalt(config.passwordLogRounds))

  def validate(password: String, hashedPassword: String) = BCryptUtils.checkpw(password, hashedPassword)
}

case class PasswordConfig(passwordLogRounds: Int)