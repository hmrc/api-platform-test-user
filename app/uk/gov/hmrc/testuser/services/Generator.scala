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

package uk.gov.hmrc.testuser.services

import org.scalacheck.Gen
import uk.gov.hmrc.domain._
import uk.gov.hmrc.testuser.models.ServiceName._
import uk.gov.hmrc.testuser.models.{ServiceName => _, _}

import scala.util.Random


trait Generator {

  private val userIdGenerator = Gen.listOfN(12, Gen.numChar).map(_.mkString)
  private val passwordGenerator = Gen.listOfN(12, Gen.alphaNumChar).map(_.mkString)
  private val utrGenerator = new SaUtrGenerator()
  private val ninoGenerator = new uk.gov.hmrc.domain.Generator()
  private val employerReferenceGenerator: Gen[EmpRef] = for {
    taxOfficeNumber <- Gen.choose(100, 999).map(x => x.toString)
    taxOfficeReference <- Gen.listOfN(10, Gen.alphaNumChar).map(_.mkString.toUpperCase)
  } yield EmpRef.fromIdentifiers(s"$taxOfficeNumber/$taxOfficeReference")
  private val vrnGenerator = Gen.choose(666000000, 666999999)
  private val arnGenerator = new ArnGenerator()
  private val mtdItIdGenerator = new MtdItIdGenerator()
  private val lisaManRefNumGenerator = new LisaGenerator()

  def generateTestIndividual(services: Seq[ServiceName] = Seq.empty) = {
    val saUtr = if (services.contains(SELF_ASSESSMENT)) Some(generateSaUtr) else None
    val nino = if (services.contains(NATIONAL_INSURANCE) || services.contains(MTD_INCOME_TAX)) Some(generateNino) else None
    val mtdItId = if(services.contains(MTD_INCOME_TAX)) Some(generateMtdId) else None

    TestIndividual(generateUserId, generatePassword, saUtr, nino, mtdItId, services)
  }

  def generateTestOrganisation(services: Seq[ServiceName] = Seq.empty) = {
    val saUtr = if (services.contains(SELF_ASSESSMENT)) Some(generateSaUtr) else None
    val nino = if (services.contains(NATIONAL_INSURANCE) || services.contains(MTD_INCOME_TAX)) Some(generateNino) else None
    val mtdItId = if (services.contains(MTD_INCOME_TAX)) Some(generateMtdId) else None
    val empRef = if (services.contains(PAYE_FOR_EMPLOYERS)) Some(generateEmpRef) else None
    val ctUtr = if (services.contains(CORPORATION_TAX)) Some(generateCtUtr) else None
    val vrn = if (services.contains(SUBMIT_VAT_RETURNS)) Some(generateVrn) else None
    val lisaManRefNum = if (services.contains(LISA)) Some(generateLisaManRefNum) else None

    TestOrganisation(generateUserId, generatePassword, saUtr, nino, mtdItId, empRef, ctUtr, vrn, lisaManRefNum, services)
  }

  def generateTestAgent(services: Seq[ServiceName] = Seq.empty) = {
    val arn = if (services.contains(AGENT_SERVICES)) Some(generateArn) else None
    TestAgent(generateUserId, generatePassword, arn, services)
  }

  private def generateUserId = userIdGenerator.sample.get
  private def generatePassword = passwordGenerator.sample.get
  private def generateEmpRef: EmpRef = employerReferenceGenerator.sample.get
  private def generateSaUtr: SaUtr = utrGenerator.nextSaUtr
  private def generateNino: Nino = ninoGenerator.nextNino
  private def generateCtUtr: CtUtr = CtUtr(utrGenerator.nextSaUtr.value)
  private def generateVrn: Vrn = Vrn(vrnGenerator.sample.get.toString)
  private def generateLisaManRefNum: LisaManagerReferenceNumber = lisaManRefNumGenerator.nextLisaManRefNum
  private def generateArn: AgentBusinessUtr = arnGenerator.nextArn
  private def generateMtdId: MtdItId = mtdItIdGenerator.nextMtdId
}

object Generator extends Generator

class ArnGenerator(random: Random = new Random) extends Modulus23Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def nextArn: AgentBusinessUtr = {
    val randomCode = "ARN" + f"${random.nextInt(1000000)}%07d"
    val checkCharacter  = calculateCheckCharacter(randomCode)
    AgentBusinessUtr(s"$checkCharacter$randomCode")
  }
}

class MtdItIdGenerator(random: Random = new Random) extends Modulus23Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def nextMtdId = {
    val randomCode = "IT" + f"${random.nextInt(1000000)}%011d"
    val checkCharacter = calculateCheckCharacter(randomCode)
    MtdItId(s"X$checkCharacter$randomCode")
  }
}

class LisaGenerator(random: Random = new Random) extends Modulus23Check {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def nextLisaManRefNum: LisaManagerReferenceNumber = {
    val randomCode = f"${random.nextInt(999999)}%06d"
    LisaManagerReferenceNumber(s"Z$randomCode")
  }
}
