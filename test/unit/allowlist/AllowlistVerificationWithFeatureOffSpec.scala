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

package unit.allowlist

import common.pages.migration.NameDobSoleTraderPage
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.api.test.Helpers._
import uk.gov.hmrc.customs.rosmfrontend.config.AppConfig
import uk.gov.hmrc.customs.rosmfrontend.controllers.migration.NameDobSoleTraderController
import uk.gov.hmrc.customs.rosmfrontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.customs.rosmfrontend.domain.subscription.NameDobDetailsSubscriptionFlowPage
import uk.gov.hmrc.customs.rosmfrontend.domain.{CdsOrganisationType, NameDobMatchModel, RegistrationDetails}
import uk.gov.hmrc.customs.rosmfrontend.models.Journey
import uk.gov.hmrc.customs.rosmfrontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.customs.rosmfrontend.views.html.migration.enter_your_details
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.subscription.SubscriptionFlowSpec
import util.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AllowlistVerificationWithFeatureOffSpec
    extends SubscriptionFlowSpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach {
  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .configure("metrics.enabled" -> false)
    .configure(Map("allowlistEnabled" -> false, "allowlist" -> "mister_allow@example.com, bob@example.com"))
    .build()

  protected override val mockSubscriptionFlowManager: SubscriptionFlowManager = mock[SubscriptionFlowManager]
  protected override val formId: String = NameDobSoleTraderPage.formId
  protected override val submitInCreateModeUrl: String =
    uk.gov.hmrc.customs.rosmfrontend.controllers.migration.routes.NameDobSoleTraderController
      .submit(isInReviewMode = false, Journey.Migrate)
      .url
  protected override val submitInReviewModeUrl: String =
    uk.gov.hmrc.customs.rosmfrontend.controllers.migration.routes.NameDobSoleTraderController
      .submit(isInReviewMode = true, Journey.Migrate)
      .url

  private val mockRequestSessionData = mock[RequestSessionData]
  private val mockRegistrationDetails = mock[RegistrationDetails](RETURNS_DEEP_STUBS)
  private val mockCdsFrontendDataCache = mock[SessionCache]
  private val enterYourDetails = app.injector.instanceOf[enter_your_details]
  private val mockConfig = mock[AppConfig]

  private val controller = new NameDobSoleTraderController(
    app,
    mockAuthConnector,
    mockSubscriptionBusinessService,
    mockRequestSessionData,
    mockCdsFrontendDataCache,
    mockSubscriptionFlowManager,
    mcc,
    enterYourDetails,
    mockSubscriptionDetailsHolderService,
    mockConfig
  )

  override def beforeEach: Unit = {
    reset(
      mockSubscriptionBusinessService,
      mockCdsFrontendDataCache,
      mockSubscriptionFlowManager,
      mockSubscriptionDetailsHolderService
    )
    when(mockSubscriptionBusinessService.cachedSubscriptionNameDobViewModel(any[Request[_]])).thenReturn(None)
    when(mockSubscriptionBusinessService.getCachedSubscriptionNameDobViewModel(any[Request[_]]))
      .thenReturn(Future.successful(NameDobSoleTraderPage.filledValues))
    when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.SoleTrader))
    when(mockSubscriptionDetailsHolderService.cacheNameDobDetails(any[NameDobMatchModel])(any[Request[_]]))
      .thenReturn(Future.successful(()))
    when(mockCdsFrontendDataCache.registrationDetails(any[Request[_]])).thenReturn(mockRegistrationDetails)
    setupMockSubscriptionFlowManager(NameDobDetailsSubscriptionFlowPage)
    when(mockConfig.autoCompleteEnabled).thenReturn(true)
  }

  "Allowlist verification" should {

    "return OK (200) when a non-allowlisted user attempts to access a route and the feature is OFF" in {
      AuthBuilder.withAuthorisedUser("user-1236213", mockAuthConnector, userEmail = Some("not@example.com"))

      val result = controller.createForm(Journey.Migrate).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

      status(result) shouldBe OK
    }

    "return OK (200) when a allowlisted user attempts to access a route and the feature is OFF" in {
      AuthBuilder.withAuthorisedUser("user-2300121", mockAuthConnector, userEmail = Some("mister_allow@example.com"))

      val result = controller.createForm(Journey.Migrate).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

      status(result) shouldBe OK
    }

    "return OK (200) when a user with no email address attempts to access a route and the feature is OFF" in {
      AuthBuilder.withAuthorisedUser("user-2300121", mockAuthConnector, userEmail = None)

      val result = controller.createForm(Journey.Migrate).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

      status(result) shouldBe OK
    }
  }
}
