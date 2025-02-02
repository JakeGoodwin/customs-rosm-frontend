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
import common.support.Env
import uk.gov.hmrc.customs.rosmfrontend.forms.models.subscription.AddressViewModel

sealed trait AddressDetailsPage extends WebPage {

  override val title = "Your details"

  val formId: String = "addressDetailsForm"

  val continueButtonXpath = "//*[@class='button']"

  val streetFieldXPath = "//*[@id='street']"
  val streetFieldLevelErrorXPath = "//*[@id='street-outer']//span[@class='error-message']"

  val cityFieldXPath = "//*[@id='city']"
  val cityFieldLevelErrorXPath = "//*[@id='city-outer']//span[@class='error-message']"

  val countryFieldLevelErrorXPath = "//*[@id='country-outer']//span[@class='error-message']"

  val countryCodeFieldXPath = "//*[@id='countryCode']"

  val postcodeFieldXPath = "//*[@id='postcode']"
  val postcodeFieldLevelErrorXPath = "//*[@id='postcode-outer']//span[@class='error-message']"

  val filledValues =
    AddressViewModel(street = "Line 1", city = "city name", postcode = Some("SW1A 2BQ"), countryCode = "ZZ")

}

trait AddressPage extends AddressDetailsPage {

  override val url: String = Env.frontendHost + "/customs/register-for-cds/address"
}

object AddressPage extends AddressPage

