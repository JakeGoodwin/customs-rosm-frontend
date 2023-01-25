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

import play.api.mvc.Request
import uk.gov.hmrc.customs.rosmfrontend.forms.models.subscription.VatEUDetailsModel
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubscriptionVatEUDetailsService @Inject()(
  subscriptionBusinessService: SubscriptionBusinessService,
  subscriptionDetailsService: SubscriptionDetailsService
) {

  def saveOrUpdate(vatEUDetail: VatEUDetailsModel)(implicit request: Request[_]): Future[Unit] =
    (subscriptionBusinessService.retrieveSubscriptionDetailsHolder map { holder =>
      subscriptionDetailsService.saveSubscriptionDetails(
        _ => holder.copy(vatEUDetails = holder.vatEUDetails ++ Seq(vatEUDetail))
      )
    }).flatten

  def saveOrUpdate(vatEUDetailsParam: Seq[VatEUDetailsModel])(implicit request: Request[_]): Future[Unit] =
    (subscriptionBusinessService.retrieveSubscriptionDetailsHolder map { holder =>
      subscriptionDetailsService.saveSubscriptionDetails(_ => holder.copy(vatEUDetails = vatEUDetailsParam))
    }).flatten

  def updateVatEuDetailsModel(oldVatEUDetail: VatEUDetailsModel, newVatEUDetail: VatEUDetailsModel)(
    implicit request: Request[_]
  ): Future[Seq[VatEUDetailsModel]] =
    cachedEUVatDetails map { cachedDetails =>
      val oldDetailsIndex = cachedDetails.indexOf(oldVatEUDetail)
      if (oldDetailsIndex >= 0) cachedDetails.updated(oldDetailsIndex, newVatEUDetail)
      else throw new IllegalArgumentException("Details for update do not exist in a cache")
    }

  def vatEuDetails(index: Int)(implicit request: Request[_]): Future[Option[VatEUDetailsModel]] =
    cachedEUVatDetails map (_.find(_.index.equals(index)))

  def cachedEUVatDetails(implicit request: Request[_]): Future[Seq[VatEUDetailsModel]] =
    subscriptionBusinessService.getCachedVatEuDetailsModel

  def removeSingleEuVatDetails(singleVatDetail: VatEUDetailsModel)(implicit request: Request[_]): Future[Unit] =
    cachedEUVatDetails.map(vatDetails => saveOrUpdate(vatDetails.filterNot(_ == singleVatDetail)))
}
