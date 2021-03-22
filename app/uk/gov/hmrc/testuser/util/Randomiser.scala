/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.testuser.util

import com.typesafe.config.Config
import org.joda.time.LocalDate

import scala.collection.JavaConverters._
import scala.util.Random

trait Randomiser {
  def config: Config

  private lazy val minimumSchoolLeavingAge = 16
  private lazy val maximumAgeOfIndividual = 100
  private lazy val ageRange = minimumSchoolLeavingAge to maximumAgeOfIndividual

  def randomNinoEligibleDateOfBirth(): LocalDate = LocalDate.now
    .withDayOfMonth(nextInt(28))
    .withMonthOfYear(nextInt(12))
    .minusYears(minimumSchoolLeavingAge + nextInt(ageRange.length))

  def randomConfigString(configKey: String): String = {
    val strings = config.getStringList(configKey).asScala
    strings(nextInt(strings.size) - 1)
  }

  private def nextInt(n: Int): Int = Random.nextInt(n) + 1

}
