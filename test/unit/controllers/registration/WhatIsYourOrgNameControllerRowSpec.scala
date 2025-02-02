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

package unit.controllers.registration

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.rosmfrontend.controllers.FeatureFlags
import uk.gov.hmrc.customs.rosmfrontend.controllers.registration.WhatIsYourOrgNameController
import uk.gov.hmrc.customs.rosmfrontend.domain.registration.UserLocation
import uk.gov.hmrc.customs.rosmfrontend.models.Journey
import uk.gov.hmrc.customs.rosmfrontend.services.cache.RequestSessionData
import uk.gov.hmrc.customs.rosmfrontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.customs.rosmfrontend.views.html.registration.what_is_your_org_name
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder
import util.builders.matching.OrganisationNameFormBuilder._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhatIsYourOrgNameControllerRowSpec extends ControllerSpec with BeforeAndAfterEach {

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("features.rowHaveUtrEnabled" -> true)
    .build()

  private val mockAuthConnector = mock[AuthConnector]
  private val mockRequestSessionData = mock[RequestSessionData]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val whatIsYourOrgNameView = app.injector.instanceOf[what_is_your_org_name]
  private val mockFeatureFlags = mock[FeatureFlags]
  private val controller = new WhatIsYourOrgNameController(
    app,
    mockAuthConnector,
    mockRequestSessionData,
    mcc,
    whatIsYourOrgNameView,
    mockSubscriptionDetailsService
  )

  override def beforeEach: Unit =
    reset(mockRequestSessionData, mockSubscriptionDetailsService)

  "Submitting the form with rowHaveUtrEnabled as true" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = false, "third-country-organisation", Journey.GetYourEORI),
      "and isInReviewMode is false"
    )
    "redirect to the 'Do you have a UTR? page when isInReviewMode is false" in {

      when(mockSubscriptionDetailsService.cacheNameDetails(any())(any[Request[_]]()))
        .thenReturn(Future.successful(()))
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]]))
        .thenReturn(Some(UserLocation.ThirdCountry))

      submitForm(isInReviewMode = false, form = ValidNameRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith(
          "/customs/register-for-cds/matching/utr-yes-no/third-country-organisation"
        )
        verify(mockSubscriptionDetailsService).cacheNameDetails(any())(any())
      }

    }

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = true, "third-country-organisation", Journey.GetYourEORI),
      "and isInReviewMode is true"
    )
    "redirect to the Determine Review page when isInReviewMode is true" in {

      when(mockSubscriptionDetailsService.cacheNameDetails(any())(any[Request[_]]()))
        .thenReturn(Future.successful(()))
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]]))
        .thenReturn(Some(UserLocation.ThirdCountry))

      submitForm(isInReviewMode = true, form = ValidNameRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("/customs/register-for-cds/matching/review-determine")
        verify(mockSubscriptionDetailsService).cacheNameDetails(any())(any())
      }
    }
  }

  def submitForm(isInReviewMode: Boolean, form: Map[String, String], userId: String = defaultUserId)(
    test: Future[Result] => Any
  ) {
    withAuthorisedUser(userId, mockAuthConnector)
    val result = controller
      .submit(isInReviewMode, "third-country-organisation", Journey.GetYourEORI)
      .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    test(result)
  }
}
