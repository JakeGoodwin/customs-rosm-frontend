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

package util.builders.matching

import uk.gov.hmrc.customs.rosmfrontend.domain.NinoMatch
import uk.gov.hmrc.customs.rosmfrontend.domain.messaging.Individual
import uk.gov.hmrc.customs.rosmfrontend.forms.MatchingForms._
import org.joda.time.LocalDate

object NinoFormBuilder {

  val FirstName = "first"
  val LastName = "last"
  val DateOfBirth = new LocalDate(1980, 3, 31)
  val Nino = "AB123456C"

  def asNinoMatch: NinoMatch = NinoMatch(FirstName, LastName, DateOfBirth, Nino)

  def asForm: Map[String, String] = ninoForm.mapping.unbind(asNinoMatch)

  def asIndividual: Individual = Individual.noMiddle(FirstName, LastName, DateOfBirth.toString)
}
