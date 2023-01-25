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

package common.pages.registration

import common.pages.WebPage

trait DoYouHaveAnEoriPage extends WebPage {

  override val fieldLevelErrorYesNoAnswer = "//*[@id='yes-no-answer-fieldset']//span[@class='error-message']"

  val url: String = "customs/register-for-cds/matching/eu-organisation"
  override val title = "What are your business details?"

}

object DoYouHaveAnEoriPage extends DoYouHaveAnEoriPage
