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

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.mvc.Request
import uk.gov.hmrc.customs.rosmfrontend.connector.SubscriptionStatusConnector
import uk.gov.hmrc.customs.rosmfrontend.domain.{SubscriptionStatusOutcome, SubscriptionStatusQueryParams}
import uk.gov.hmrc.customs.rosmfrontend.services.RequestCommonGenerator
import uk.gov.hmrc.customs.rosmfrontend.services.cache.SessionCache
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubscriptionStatusService @Inject()(
  connector: SubscriptionStatusConnector,
  requestCommonGenerator: RequestCommonGenerator,
  cache: SessionCache,
) {

  private val dateFormat = DateTimeFormat.forPattern("d MMM yyyy")

  def getStatus(idType: String, id: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[PreSubscriptionStatus] = {

    def createRequest =
      SubscriptionStatusQueryParams(requestCommonGenerator.receiptDate, "CDS", idType, id)

    def saveToCache(processingDate: DateTime) = cache.saveSubscriptionStatusOutcome(SubscriptionStatusOutcome(dateFormat.print(processingDate)))

    def checkSubscriptionStatus() =
      connector.status(createRequest).map { response =>
        saveToCache(response.responseCommon.processingDate)
        response.responseDetail.subscriptionStatus match {
          case "00" => NewSubscription
          case "01" => SubscriptionProcessing
          case "04" => SubscriptionExists
          case "05" => SubscriptionRejected
          case "11" => SubscriptionProcessing
          case "14" => SubscriptionProcessing
          case "99" => SubscriptionRejected
        }
      }

    checkSubscriptionStatus()

  }
}

sealed trait PreSubscriptionStatus

case object NewSubscription extends PreSubscriptionStatus

case object SubscriptionRejected extends PreSubscriptionStatus

case object SubscriptionProcessing extends PreSubscriptionStatus

case object SubscriptionExists extends PreSubscriptionStatus
