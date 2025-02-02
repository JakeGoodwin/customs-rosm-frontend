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

package unit.controllers.subscription

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.rosmfrontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.customs.rosmfrontend.domain.subscription.{SubscriptionFlowInfo, SubscriptionPage}
import uk.gov.hmrc.customs.rosmfrontend.services.subscription.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec

trait SubscriptionFlowTestSupport extends ControllerSpec {

  protected def formId: String

  protected val nextPageUrl = "next-page-url"
  protected val nextPage = mock[SubscriptionPage]
  protected val subscriptionFlowStepInfo = SubscriptionFlowInfo(stepNumber = 7, totalSteps = 10, nextPage = nextPage)

  protected val mockSubscriptionFlowManager = mock[SubscriptionFlowManager]
  protected val mockAuthConnector = mock[AuthConnector]
  protected val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]
  protected val mockSubscriptionDetailsHolderService = mock[SubscriptionDetailsService]

  def setupMockSubscriptionFlowManager(currentPage: SubscriptionPage): Unit = {
    when(nextPage.url).thenReturn(nextPageUrl)
    when(mockSubscriptionFlowManager.stepInformation(meq(currentPage))(any[HeaderCarrier], any[Request[AnyContent]]))
      .thenReturn(subscriptionFlowStepInfo)
  }
}
