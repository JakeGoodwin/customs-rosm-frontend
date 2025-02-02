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

package common.pages.subscription

import common.pages.WebPage
import common.support.Env.frontendHost

trait DisclosePersonalDetailsConsentPage extends WebPage {

  override val url: String = frontendHost + "/customs/register-for-cds/disclose-personal-details-consent"

  override val fieldLevelErrorYesNoAnswer: String = "//*[@id='yes-no-answer-field']//span[@class='error-message']"

  override val title = "Do you want to show your organisation name and address on the check an EORI number service?"

  val formId: String = "disclose-personal-details-createForm"


  val consentInfoXpath = "//div[@class='column-two-thirds']//p"

  val yesToDiscloseXpath = "//label[@for='yes-no-answer-true']"
  val noToDiscloseXpath = "//label[@for='yes-no-answer-false']"
  val yesToDiscloseInputXpath = "//*[@id='yes-no-answer-true']"
  val noToDiscloseInputXpath = "//*[@id='yes-no-answer-false']"

  val continueButtonXpath = "//*[@class='button']"

}

object DisclosePersonalDetailsConsentPage extends DisclosePersonalDetailsConsentPage

