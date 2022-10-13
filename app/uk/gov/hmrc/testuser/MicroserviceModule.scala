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

package uk.gov.hmrc.testuser

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

import javax.inject.{Inject, Provider}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.testuser.services.PasswordConfig
import play.api.inject.Module

class MicroserviceModule(val environment: Environment, val configuration: Configuration) extends Module {

  override def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[PasswordConfig].toProvider(classOf[PasswordConfigProvider])
  )
}

class PasswordConfigProvider @Inject() (val runModeConfiguration: Configuration, environment: Environment, config: ServicesConfig)
    extends Provider[PasswordConfig] {

  import config.getInt

  override def get(): PasswordConfig = {
    val passwordLogRounds = getInt("passwordLogRounds")
    PasswordConfig(passwordLogRounds)
  }
}
