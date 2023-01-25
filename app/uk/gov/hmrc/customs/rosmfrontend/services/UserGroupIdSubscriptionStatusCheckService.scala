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

package uk.gov.hmrc.customs.rosmfrontend.services

import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.customs.rosmfrontend.config.AppConfig
import uk.gov.hmrc.customs.rosmfrontend.connector.Save4LaterConnector
import uk.gov.hmrc.customs.rosmfrontend.controllers.auth.EnrolmentExtractor
import uk.gov.hmrc.customs.rosmfrontend.domain._
import uk.gov.hmrc.customs.rosmfrontend.forms.models.email.EmailStatus
import uk.gov.hmrc.customs.rosmfrontend.logging.CdsLogger
import uk.gov.hmrc.customs.rosmfrontend.models.Journey
import uk.gov.hmrc.customs.rosmfrontend.services.cache.CachedData
import uk.gov.hmrc.customs.rosmfrontend.services.subscription._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserGroupIdSubscriptionStatusCheckService @Inject()(
  subscriptionStatusService: SubscriptionStatusService,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  save4LaterConnector: Save4LaterConnector,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
  extends EnrolmentExtractor {
  private val idType = "SAFE"

  def checksToProceed(
    groupId: GroupId,
    internalId: InternalId,
    redirectSubToECC: Boolean,
    redirectRegToECC: Boolean,
    journey: Journey.Value
  )(continue: => Future[Result])
   (groupIsEnrolled: => Future[Result])
   (userIsInProcess: => Future[Result])
   (existingApplicationInProcess: => Future[Result])
   (otherUserWithinGroupIsInProcess: => Future[Result])
   (implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    enrolmentStoreProxyService.isEnrolmentAssociatedToGroup(groupId).flatMap {
      case true => groupIsEnrolled //Block the user
      case false =>
        save4LaterConnector
          .get[CacheIds](groupId.id, CachedData.groupIdKey)
          .flatMap {
            case Some(cacheIds) =>
              journey match {
                case Journey.GetYourEORI =>
                    processUserWithCachedId(cacheIds, groupId, internalId)
                      {continue}{userIsInProcess}{existingApplicationInProcess}{otherUserWithinGroupIsInProcess}
                case Journey.Migrate =>
                  redirectUser(journey, redirectSubToECC, redirectRegToECC, internalId) {
                    processUserWithCachedId(cacheIds, groupId, internalId)
                      {continue}{userIsInProcess}{existingApplicationInProcess}{otherUserWithinGroupIsInProcess}
                  }
              }
            case _ => redirectUser(journey, redirectSubToECC, redirectRegToECC, internalId)(continue)
          }
    }
  }

  private def processUserWithCachedId(cacheIds: CacheIds, groupId: GroupId, internalId: InternalId)
    (continue: => Future[Result])
    (userIsInProcess: => Future[Result])
    (existingApplicationInProcess: => Future[Result])
    (otherUserWithinGroupIsInProcess: => Future[Result])
    (implicit request: Request[AnyContent], hc: HeaderCarrier) = {
  subscriptionStatusService.getStatus(idType, cacheIds.safeId.id)
    .flatMap {
      case NewSubscription | SubscriptionRejected =>
        save4LaterConnector.delete(groupId.id).flatMap(_ => continue)
      case SubscriptionProcessing =>
        if (cacheIds.internalId == internalId) {
          existingApplicationInProcess
        } else {
          otherUserWithinGroupIsInProcess
        }
      case _ =>
        if (cacheIds.internalId == internalId) {
          userIsInProcess
        } else {
          otherUserWithinGroupIsInProcess
        }
    }
  }

  private def redirectUser(
    journey: Journey.Value,
    redirectSubToECC: Boolean,
    redirectRegToECC: Boolean,
    internalId: InternalId
  ) (action: => Future[Result])
    (implicit request: Request[AnyContent], hc: HeaderCarrier) =
    journey match {
      case Journey.GetYourEORI =>
          save4LaterConnector.get[EmailStatus](internalId.id, CachedData.emailKey).flatMap {
            case None if redirectRegToECC =>
              CdsLogger.info(s"redirectRegToECC flag is enabled and no email cached - redirecting to ECC")
              Future.successful(Redirect(appConfig.eccRegistrationEntryPoint))
            case _ =>
              CdsLogger.info(s"redirectRegToECC flag is $redirectRegToECC and no email cached - continue to CDS")
              action
          }
      case Journey.Migrate =>
        if (redirectSubToECC) {
          CdsLogger.debug("redirectToECCEnabled flag is enabled. Redirecting to ECC")
          Future.successful(Redirect(appConfig.subscribeLinkSubscribe))
        } else {
          CdsLogger.info("redirectToECCEnabled flag is disabled. Continuing in CDS service")
          action
        }
    }

  def userOrGroupHasAnEori(
    groupId: GroupId
  )(implicit request: Request[AnyContent],
    user: LoggedInUserWithEnrolments,
    hc: HeaderCarrier): Future[Option[Eori]] =
    enrolmentStoreProxyService.groupIdEnrolments(groupId).map {
      existingEoriForUserOrGroup(user, _)
    }
}
