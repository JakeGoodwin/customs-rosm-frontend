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

package uk.gov.hmrc.customs.rosmfrontend.services.countries

import com.google.inject.Inject
import play.api._
import play.api.libs.json._
import uk.gov.hmrc.customs.rosmfrontend.domain.registration.UserLocation

import javax.inject.Singleton
import scala.io.Source

@Singleton
class Countries @Inject()(app: Application) {
  private val countriesFilename: String =
    app.configuration.get[String]("countriesFilename")
  private val mdgCountryCodesFilename: String =
    app.configuration.get[String]("mdgCountryCodesFilename")
  private val mdgNotIomCountryCodesFilename: String =
    app.configuration.get[String]("mdgNotIomCountryCodesFilename")
  private val mdgEuCountryCodesFilename: String =
    app.configuration.get[String]("mdgEuCountryCodesFilename")
  private val mdgThirdCountryCodesFilename: String =
    app.configuration.get[String]("mdgThirdCountryCodesFilename")
  private val mdgIslandsCountryCodesFilename: String =
    app.configuration.get[String]("mdgIslandsCountryCodesFilename")
  private def mdgCountryCodes(fileName: String): List[String] =
    Source
      .fromInputStream(app.classloader.getResourceAsStream(fileName))
      .getLines()
      .mkString
      .split(',')
      .map(_.replace("\"", ""))
      .toList

  private val countries: List[Country] = {
    def fromJsonFile: List[Country] =
      Json.parse(app.classloader.getResourceAsStream(countriesFilename)) match {
        case JsArray(cs) =>
          cs.toList.collect {
            case JsArray(Seq(c: JsString, cc: JsString)) =>
              Country(c.value, countryCode(cc.value))
          }
        case _ =>
          throw new IllegalArgumentException("Could not read JSON array of countries from : " + countriesFilename)
      }

    fromJsonFile.sortBy(_.countryName)
  }

  private def countryCode: String => String = cc => cc.split(":")(1).trim

  val all: List[Country] = countries filter (c => mdgCountryCodes(mdgCountryCodesFilename) contains c.countryCode)
  val allExceptIom: List[Country] = countries filter (
    c => mdgCountryCodes(mdgNotIomCountryCodesFilename) contains c.countryCode
  )
  val eu: List[Country] = countries filter (c => mdgCountryCodes(mdgEuCountryCodesFilename) contains c.countryCode)
  val third: List[Country] = countries filter (
    c => mdgCountryCodes(mdgThirdCountryCodesFilename) contains c.countryCode
  )
  val islands: List[Country] = countries filter (
    c => mdgCountryCodes(mdgIslandsCountryCodesFilename) contains c.countryCode
  )

  def getCountryParameters(location: Option[String]): (List[Country], CountriesInCountryPicker) = location match {
    case Some(UserLocation.Eu) => (eu, EUCountriesInCountryPicker)
    case Some(UserLocation.ThirdCountry) =>
      (third, ThirdCountriesInCountryPicker)
    case Some(UserLocation.Islands) => (islands, IslandsInCountryPicker)
    case _                          => (allExceptIom, AllCountriesExceptIomInCountryPicker)
  }

  def getCountryParametersForAllCountries(): (List[Country], CountriesInCountryPicker) =
    (all, AllCountriesInCountryPicker)
}

sealed trait CountriesInCountryPicker

case object AllCountriesInCountryPicker extends CountriesInCountryPicker
case object AllCountriesExceptIomInCountryPicker extends CountriesInCountryPicker
case object EUCountriesInCountryPicker extends CountriesInCountryPicker
case object ThirdCountriesInCountryPicker extends CountriesInCountryPicker
case object IslandsInCountryPicker extends CountriesInCountryPicker
case object NoCountriesInCountryPicker extends CountriesInCountryPicker
