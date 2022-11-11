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

package uk.gov.hmrc.customs.rosmfrontend.services.subscription

import org.joda.time.LocalDate
import play.api.mvc.Request
import uk.gov.hmrc.customs.rosmfrontend.domain._
import uk.gov.hmrc.customs.rosmfrontend.domain.subscription._
import uk.gov.hmrc.customs.rosmfrontend.forms.models.subscription.{AddressViewModel, ContactDetailsModel, VatDetails, VatEUDetailsModel}
import uk.gov.hmrc.customs.rosmfrontend.services.cache.SessionCache

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubscriptionBusinessService @Inject()(
  cdsFrontendDataCache: SessionCache
) {

  def getCachedCompanyShortName(implicit request: Request[_]): Future[BusinessShortName] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.businessShortName.getOrElse(throw new IllegalStateException("No Short Name Cached"))
    }

  def companyShortName(implicit request: Request[_]): Future[Option[BusinessShortName]] =
    cdsFrontendDataCache.subscriptionDetails map (_.businessShortName)

  def cachedContactDetailsModel(implicit request: Request[_]): Future[Option[ContactDetailsModel]] =
    cdsFrontendDataCache.subscriptionDetails map (_.contactDetails)

  def getCachedDateEstablished(implicit request: Request[_]): Future[LocalDate] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.dateEstablished.getOrElse(throw new IllegalStateException("No Date Of Establishment Cached"))
    }

  def maybeCachedDateEstablished(implicit request: Request[_]): Future[Option[LocalDate]] =
    cdsFrontendDataCache.subscriptionDetails map (_.dateEstablished)

  def getCachedSicCode(implicit request: Request[_]): Future[String] = cdsFrontendDataCache.subscriptionDetails map {
    _.sicCode.getOrElse(throw new IllegalStateException("No SIC Code Cached"))
  }

  def cachedSicCode(implicit request: Request[_]): Future[Option[String]] =
    cdsFrontendDataCache.subscriptionDetails map (_.sicCode)

  def getCachedEoriNumber(implicit request: Request[_]): Future[String] = cdsFrontendDataCache.subscriptionDetails map {
    _.eoriNumber.getOrElse(throw new IllegalStateException("No Eori Number Cached"))
  }

  def cachedEoriNumber(implicit request: Request[_]): Future[Option[String]] =
    cdsFrontendDataCache.subscriptionDetails map (_.eoriNumber)

  def getCachedPersonalDataDisclosureConsent(implicit request: Request[_]): Future[Boolean] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.personalDataDisclosureConsent.getOrElse(
        throw new IllegalStateException("No Personal Data Disclosure Consent Cached")
      )
    }

  def getCachedVatRegisteredUk(implicit request: Request[_]): Future[Boolean] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.vatRegisteredUk.getOrElse(
        throw new IllegalStateException("Whether the business is VAT registered in the UK has not been Cached")
      )
    }

  def getCachedVatRegisteredEu(implicit request: Request[_]): Future[Boolean] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.vatRegisteredEu.getOrElse(
        throw new IllegalStateException("Whether the business is VAT registered in the EU has not been Cached")
      )
    }

  def getCachedVatGroup(implicit request: Request[_]): Future[Option[Boolean]] =
    cdsFrontendDataCache.subscriptionDetails map {
      _.vatGroup
    }

  def addressOrException(implicit request: Request[_]): Future[AddressViewModel] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.addressDetails.getOrElse(throw new IllegalStateException("No Address Details Cached"))
    }

  def address(implicit request: Request[_]): Future[Option[AddressViewModel]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.addressDetails
    }

  def getCachedNameIdViewModel(implicit request: Request[_]): Future[NameIdOrganisationMatchModel] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.nameIdOrganisationDetails.getOrElse(
        throw new IllegalStateException("No Name/Utr/Id Details Cached")
      )
    }

  def getCachedNameViewModel(implicit request: Request[_]): Future[NameOrganisationMatchModel] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.nameOrganisationDetails.getOrElse(throw new IllegalStateException("No Name Cached"))
    }

  def cachedNameIdOrganisationViewModel(implicit request: Request[_]): Future[Option[NameIdOrganisationMatchModel]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.nameIdOrganisationDetails
    }

  def cachedNameOrganisationViewModel(implicit request: Request[_]): Future[Option[NameOrganisationMatchModel]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.nameOrganisationDetails
    }

  def getCachedSubscriptionNameDobViewModel(implicit request: Request[_]): Future[NameDobMatchModel] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.nameDobDetails.getOrElse(throw new IllegalStateException("No Name/Dob Details Cached"))
    }

  def cachedSubscriptionNameDobViewModel(implicit request: Request[_]): Future[Option[NameDobMatchModel]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.nameDobDetails
    }

  def getCachedCustomsId(implicit request: Request[_]): Future[Option[CustomsId]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.customsId
    }

  def getCachedSubscriptionIdViewModel(implicit request: Request[_]): Future[IdMatchModel] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.idDetails.getOrElse(throw new IllegalStateException("No Nino/Id Details Cached"))
    }

  def maybeCachedSubscriptionIdViewModel(implicit request: Request[_]): Future[Option[IdMatchModel]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.idDetails
    }

  def getCachedUkVatDetails(implicit request: Request[_]): Future[Option[VatDetails]] =
    cdsFrontendDataCache.subscriptionDetails map { subscriptionDetails =>
      subscriptionDetails.ukVatDetails
    }

  def getCachedVatEuDetailsModel(implicit request: Request[_]): Future[Seq[VatEUDetailsModel]] =
    cdsFrontendDataCache.subscriptionDetails map (_.vatEUDetails)

  def retrieveSubscriptionDetailsHolder(implicit request: Request[_]): Future[SubscriptionDetails] =
    cdsFrontendDataCache.subscriptionDetails
}
