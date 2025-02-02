/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.customs.rosmfrontend.domain.Utr
import uk.gov.hmrc.customs.rosmfrontend.domain.messaging.matching.Organisation

object NameIdOrganisationFormBuilder {

  val ValidUtrId: String =  "1111111111"

  val ValidUtr = Utr(ValidUtrId)
  val ValidName = "SA Partnership 3 For Digital"
  val ValidNameUtrRequest = Map("name" -> ValidName, "utr" -> ValidUtrId)

  val CompanyOrganisation = Organisation(ValidName, "Corporate Body")
  val LimitedLiabilityPartnershipOrganisation = Organisation(ValidName, "LLP")
  val PartnershipOrganisation = Organisation(ValidName, "Partnership")
  val CharityPublicBodyNotForProfitOrganisation = Organisation(ValidName, "Unincorporated Body")
}
