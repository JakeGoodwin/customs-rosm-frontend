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

package uk.gov.hmrc.customs.rosmfrontend.services.subscription

import org.joda.time.LocalDate
import play.api.mvc.Request
import uk.gov.hmrc.customs.rosmfrontend.connector.Save4LaterConnector
import uk.gov.hmrc.customs.rosmfrontend.domain._
import uk.gov.hmrc.customs.rosmfrontend.domain.subscription.{BusinessShortName, SubscriptionDetails}
import uk.gov.hmrc.customs.rosmfrontend.forms.models.subscription.{AddressViewModel, ContactDetailsModel, VatDetails}
import uk.gov.hmrc.customs.rosmfrontend.services.cache.{CachedData, SessionCache}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubscriptionDetailsService @Inject()(
  sessionCache: SessionCache,
  save4LaterConnector: Save4LaterConnector
) {

  def saveKeyIdentifiers(groupId: GroupId, internalId: InternalId)(implicit hc: HeaderCarrier, request: Request[_]): Future[Unit] = {
    val key = CachedData.groupIdKey
    sessionCache.safeId.flatMap { safeId =>
      val cacheIds = CacheIds(internalId, safeId)
      save4LaterConnector.put[CacheIds](groupId.id, key, cacheIds)
    }
  }

  def saveSubscriptionDetails(
    insertNewDetails: SubscriptionDetails => SubscriptionDetails
  )(implicit request: Request[_]): Future[Unit] = sessionCache.subscriptionDetails flatMap { subDetails =>
    sessionCache.saveSubscriptionDetails(insertNewDetails(subDetails)).map(_ => ())

  }

  def cacheCompanyShortName(shortName: BusinessShortName)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(businessShortName = Some(shortName)))

  def cacheContactDetails(contactDetails: ContactDetailsModel)(
    implicit request: Request[_]
  ): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(contactDetails = Some(contactDetails)))

  def cacheAddressDetails(address: AddressViewModel)(implicit request: Request[_]): Future[Unit] = {
    def noneForEmptyPostcode(a: AddressViewModel) = a.copy(postcode = a.postcode.filter(_.nonEmpty))
    saveSubscriptionDetails(sd => sd.copy(addressDetails = Some(noneForEmptyPostcode(address))))
  }

  def cachedAddressDetails(implicit request: Request[_]): Future[Option[AddressViewModel]] =
    sessionCache.subscriptionDetails map (_.addressDetails)

  def cacheNameIdDetails(
    nameIdOrganisationMatchModel: NameIdOrganisationMatchModel
  )(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(nameIdOrganisationDetails = Some(nameIdOrganisationMatchModel)))

  def cacheNameIdAndCustomsId(name: String, id: String)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(
      sd => sd.copy(nameIdOrganisationDetails = Some(NameIdOrganisationMatchModel(name, id)), customsId = Some(Utr(id)))
    )

  def cachedNameIdDetails(implicit request: Request[_]): Future[Option[NameIdOrganisationMatchModel]] =
    sessionCache.subscriptionDetails map (_.nameIdOrganisationDetails)

  def cacheNameDetails(
    nameOrganisationMatchModel: NameOrganisationMatchModel
  )(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(nameOrganisationDetails = Some(nameOrganisationMatchModel)))

  def cachedNameDetails(implicit request: Request[_]): Future[Option[NameOrganisationMatchModel]] =
    sessionCache.subscriptionDetails map (_.nameOrganisationDetails)

  def cacheDateOfBirth(date: LocalDate)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(dateOfBirth = Some(date)))

  def cacheSicCode(sicCode: String)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(sicCode = Some(sicCode)))

  def cacheEoriNumber(eoriNumber: String)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(eoriNumber = Some(eoriNumber)))

  def cacheDateEstablished(date: LocalDate)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(dateEstablished = Some(date)))

  def cachePersonalDataDisclosureConsent(consent: Boolean)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(personalDataDisclosureConsent = Some(consent)))

  def cacheNameDobDetails(nameDob: NameDobMatchModel)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(nameDobDetails = Some(nameDob)))

  def cachedNameDobDetails(implicit equest: Request[_]): Future[Option[NameDobMatchModel]] =
    sessionCache.subscriptionDetails.map(_.nameDobDetails)

  def cacheIdDetails(idMatchModel: IdMatchModel)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(idDetails = Some(idMatchModel)))

  def cacheCustomsId(subscriptionCustomsId: CustomsId)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(customsId = Some(subscriptionCustomsId)))

  def clearCachedCustomsId(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(customsId = None))

  def cacheUkVatDetails(ukVatDetails: VatDetails)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(ukVatDetails = Some(ukVatDetails)))

  def clearCachedUkVatDetails(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(ukVatDetails = None))

  def cacheVatRegisteredUk(yesNoAnswer: YesNo)(implicit request: Request[_]) =
    saveSubscriptionDetails(sd => sd.copy(vatRegisteredUk = Some(yesNoAnswer.isYes)))

  def cacheVatGroup(yesNoAnswer: YesNo)(implicit request: Request[_]) =
    saveSubscriptionDetails(sd => sd.copy(vatGroup = Some(yesNoAnswer.isYes)))

  def cacheConsentToDisclosePersonalDetails(yesNoAnswer: YesNo)(implicit request: Request[_]) =
    saveSubscriptionDetails(sd => sd.copy(personalDataDisclosureConsent = Some(yesNoAnswer.isYes)))

  def cacheVatRegisteredEu(yesNoAnswer: YesNo)(implicit request: Request[_]): Future[Unit] =
    for {
      existingHolder <- sessionCache.subscriptionDetails
      updatedHolder = existingHolder.copy(vatRegisteredEu = Some(yesNoAnswer.isYes))
      _ <- sessionCache.saveSubscriptionDetails(updatedHolder)
    } yield ()

  def cachedCustomsId(implicit request: Request[_]): Future[Option[CustomsId]] =
    sessionCache.subscriptionDetails map (_.customsId)

  def cacheExistingEoriNumber(eori: String)(implicit request: Request[_]): Future[Unit] =
    saveSubscriptionDetails(sd => sd.copy(existingEoriNumber = Some(eori)))

  def cachedExistingEoriNumber(implicit request: Request[_]): Future[Option[String]] =
    sessionCache.subscriptionDetails map (_.existingEoriNumber)

  def updateSubscriptionDetails(implicit request: Request[_]) =
    sessionCache.subscriptionDetails flatMap { subDetails =>
      sessionCache.saveRegistrationDetails(RegistrationDetailsOrganisation())
      sessionCache.saveRegistrationDetails(RegistrationDetailsIndividual())
      sessionCache.saveSubscriptionStatusOutcome(SubscriptionStatusOutcome(""))
      sessionCache.saveSubscriptionDetails(
        SubscriptionDetails(
          nameOrganisationDetails = subDetails.nameOrganisationDetails,
          nameDobDetails = subDetails.nameDobDetails
        )
      )
    }
}
