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

import common.pages.matching.OrganisationUtrPage._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.rosmfrontend.connector.MatchingServiceConnector
import uk.gov.hmrc.customs.rosmfrontend.controllers.registration.WhatIsYourUtrNumberController
import uk.gov.hmrc.customs.rosmfrontend.domain._
import uk.gov.hmrc.customs.rosmfrontend.domain.messaging.Individual
import uk.gov.hmrc.customs.rosmfrontend.domain.messaging.matching.{MatchingRequestHolder, MatchingResponse, Organisation}
import uk.gov.hmrc.customs.rosmfrontend.models.Journey
import uk.gov.hmrc.customs.rosmfrontend.services.registration.MatchingService
import uk.gov.hmrc.customs.rosmfrontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.customs.rosmfrontend.views.html.registration.match_organisation_utr
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder
import util.builders.matching.OrganisationUtrFormBuilder._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhatIsYourUtrNumberControllerSpec extends ControllerSpec with MockitoSugar with BeforeAndAfterEach {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockMatchingService = mock[MatchingService]
  private val mockMatchingConnector = mock[MatchingServiceConnector]
  private val mockMatchingRequestHolder = mock[MatchingRequestHolder]
  private val mockMatchingResponse = mock[MatchingResponse]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val matchOrganisationUtrView = app.injector.instanceOf[match_organisation_utr]

  private val controller = new WhatIsYourUtrNumberController(
    app,
    mockAuthConnector,
    mockMatchingService,
    mcc,
    matchOrganisationUtrView,
    mockSubscriptionDetailsService
  )

  private val UtrInvalidError = "Enter a Unique Taxpayer Reference in the correct format"
  private val BusinessNotMatchedError =
    "Your business details have not been found. Check that your details are correct and try again."
  private val IndividualNotMatchedError =
    "Your details have not been found. Check that your details are correct and then try again."

  override def beforeEach: Unit =
    reset(mockMatchingService, mockSubscriptionDetailsService)

  "Viewing the Utr Organisation Matching form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.form(CdsOrganisationType.CharityPublicBodyNotForProfitId, Journey.GetYourEORI)
    )

    "display the form" in {
      showForm(CdsOrganisationType.CharityPublicBodyNotForProfitId) { result =>
        status(result) shouldBe OK
        val page = CdsPage(bodyOf(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
        page.getElementsText(fieldLevelErrorUtr) shouldBe empty

      }
    }

    "ensure the labels are correct for CdsOrganisationType.CharityPublicBodyNotForProfitId" in {
      submitForm(form = ValidUtrRequest + ("utr" -> ""), CdsOrganisationType.CharityPublicBodyNotForProfitId) {
        result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(bodyOf(result))

          page.getElementsText("//*[@id='utr-outer']//span[@class='error-message']") shouldBe "Error: Enter your Unique Taxpayer Reference"
      }
    }
  }

  "Submitting the form for Organisation Types that have a UTR" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(CdsOrganisationType.CharityPublicBodyNotForProfitId, Journey.GetYourEORI)
    )

    "ensure UTR has been entered when organisation type is 'CdsOrganisationType.CharityPublicBodyNotForProfitId'" in {
      submitForm(form = ValidUtrRequest + ("utr" -> ""), CdsOrganisationType.CharityPublicBodyNotForProfitId) {
        result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(bodyOf(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your Unique Taxpayer Reference"
          page.getElementsText(fieldLevelErrorUtr) shouldBe "Error: Enter your Unique Taxpayer Reference"
          page.getElementsText("title") should startWith("Error: ")
      }
    }

    "ensure when UTR is correctly formatted it is a valid UTR when organisation type is 'CdsOrganisationType.CharityPublicBodyNotForProfitId'" in {
      val invalidUtr = "0123456789"
      submitForm(form = ValidUtrRequest + ("utr" -> invalidUtr), CdsOrganisationType.CharityPublicBodyNotForProfitId) {
        result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(bodyOf(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe UtrInvalidError
          page.getElementsText(fieldLevelErrorUtr) shouldBe s"Error: $UtrInvalidError"
          page.getElementsText("title") should startWith("Error: ")
      }
    }

    "send a request to the business matching service when organisation type is 'CdsOrganisationType.CharityPublicBodyNotForProfitId'" in {
      when(mockSubscriptionDetailsService.cachedNameDetails(any[Request[_]]))
        .thenReturn(Future.successful(Some(NameOrganisationMatchModel("orgName"))))
      when(
        mockMatchingService.matchBusiness(any[Utr], any[Organisation], any[Option[LocalDate]], any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(true))
      submitForm(ValidUtrRequest, CdsOrganisationType.CharityPublicBodyNotForProfitId) { result =>
        await(result)
        verify(mockMatchingService).matchBusiness(
          meq(ValidUtr),
          meq(charityPublicBodyNotForProfitOrganisation),
          meq(None),
          any()
        )(any[Request[AnyContent]], any[HeaderCarrier])
      }
    }

    "return a Bad Request when business match is unsuccessful" in {
      when(mockSubscriptionDetailsService.cachedNameDetails(any[Request[_]]))
        .thenReturn(Future.successful(Some(NameOrganisationMatchModel("orgName"))))
      when(
        mockMatchingService.matchBusiness(
          meq(ValidUtr),
          meq(charityPublicBodyNotForProfitOrganisation),
          meq(None),
          any()
        )(any[Request[AnyContent]], any[HeaderCarrier])
      ).thenReturn(Future.successful(false))
      submitForm(ValidUtrRequest, CdsOrganisationType.CharityPublicBodyNotForProfitId) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(bodyOf(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe BusinessNotMatchedError
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "redirect to the confirm page when match is successful" in {
      when(mockSubscriptionDetailsService.cachedNameDetails(any[Request[_]]))
        .thenReturn(Future.successful(Some(NameOrganisationMatchModel("orgName"))))
      when(
        mockMatchingService.matchBusiness(
          meq(ValidUtr),
          meq(charityPublicBodyNotForProfitOrganisation),
          meq(None),
          any()
        )(any[Request[AnyContent]], any[HeaderCarrier])
      ).thenReturn(Future.successful(true))
      submitForm(ValidUtrRequest, CdsOrganisationType.CharityPublicBodyNotForProfitId) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("/customs/register-for-cds/matching/confirm")
      }
    }
  }

  "display the form for ROW organisation" should {

    "when ThirdCountryOrganisationId is passed" in {
      showForm(CdsOrganisationType.ThirdCountryOrganisationId) { result =>
        val page = CdsPage(bodyOf(result))
        page.title should startWith(
          "What is your Corporation Tax Unique Taxpayer Reference?"
        )
        page.h1 shouldBe "What is your Corporation Tax Unique Taxpayer Reference? This is 10 numbers, for example 1234567890. It will be on tax returns and other letters about Corporation Tax. It may be called ‘reference’, ‘UTR’ or ‘official use’. You can find a lost UTR number."

        page.getElementsText("//*[@class='form-hint']") shouldBe "This is 10 numbers, for example 1234567890. It will be on tax returns and other letters about Corporation Tax. It may be called ‘reference’, ‘UTR’ or ‘official use’. You can find a lost UTR number."
      }
    }
  }

  "submitting the form for ROW organisation" should {
    "redirect to Confirm Details page based on YES answer" in {
      when(mockSubscriptionDetailsService.cachedNameDetails(any[Request[_]]))
        .thenReturn(Future.successful(Some(NameOrganisationMatchModel("orgName"))))
      when(
        mockMatchingService.matchBusiness(meq(ValidUtr), meq(thirdCountryOrganisation), meq(None), any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(true))

      submitForm(form = ValidUtrRequest, CdsOrganisationType.ThirdCountryOrganisationId) { result =>
        await(result)
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("register-for-cds/matching/confirm")
        verify(mockMatchingService).matchBusiness(meq(ValidUtr), meq(thirdCountryOrganisation), meq(None), any())(
          any[Request[AnyContent]],
          any[HeaderCarrier]
        )
      }
    }
  }

  "display the form for ROW" should {
    "contain a proper content for sole traders" in {
      showForm(CdsOrganisationType.ThirdCountrySoleTraderId, defaultUserId) { result =>
        val page = CdsPage(bodyOf(result))
        page.title should startWith(
          "What is your Self Assessment Unique Taxpayer Reference?"
        )
        page.h1 shouldBe "What is your Self Assessment Unique Taxpayer Reference? This is 10 numbers, for example 1234567890. " +
          "It will be on tax returns and other letters about Self Assessment. It may be called ‘reference’, ‘UTR’ or ‘official use’. You can find a lost UTR number."
        page.getElementsText("//*[@class='form-hint']") shouldBe "This is 10 numbers, for example 1234567890. " +
          "It will be on tax returns and other letters about Self Assessment. It may be called ‘reference’, ‘UTR’ or ‘official use’. You can find a lost UTR number."
      }
    }
    "contain a proper content for individuals" in {
      showForm(CdsOrganisationType.ThirdCountryIndividualId, defaultUserId) { result =>
        val page = CdsPage(bodyOf(result))
        page.title should startWith(
          "What is your Self Assessment Unique Taxpayer Reference?"
        )
        page.h1 shouldBe "What is your Self Assessment Unique Taxpayer Reference? This is 10 numbers, for example 1234567890. " +
          "It will be on tax returns and other letters about Self Assessment. It may be called ‘reference’, ‘UTR’ or ‘official use’. You can find a lost UTR number."
        page.getElementsText("//*[@class='form-hint']") shouldBe "This is 10 numbers, for example 1234567890. " +
          "It will be on tax returns and other letters about Self Assessment. It may be called ‘reference’, ‘UTR’ or ‘official use’. You can find a lost UTR number."
      }
    }
  }

  "submitting the form for ROW" should {
    "redirect to Confirm Details page based on YES answer and organisation type sole trader" in {
      when(mockSubscriptionDetailsService.cachedNameDobDetails(any[Request[_]]))
        .thenReturn(Future.successful(Some(NameDobMatchModel("", None, "", LocalDate.now()))))
      when(mockMatchingConnector.lookup(mockMatchingRequestHolder))
        .thenReturn(Future.successful(Option(mockMatchingResponse)))
      when(mockMatchingService.matchIndividualWithId(meq(ValidUtr), any[Individual], any())(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful(true))
      submitForm(form = ValidUtrRequest, CdsOrganisationType.ThirdCountrySoleTraderId) { result =>
        await(result)
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("register-for-cds/matching/confirm")
      }
    }

    "redirect to bad request page when cachedNameDobDetails is None" in {
      when(mockSubscriptionDetailsService.cachedNameDobDetails(any[Request[_]])).thenReturn(Future.successful(None))
      when(mockMatchingService.matchIndividualWithId(meq(ValidUtr), any[Individual], any())(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful(false))
      submitForm(ValidUtrRequest, CdsOrganisationType.ThirdCountrySoleTraderId) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(bodyOf(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe IndividualNotMatchedError
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "redirect to bad request page when orgName not found" in {
      when(mockSubscriptionDetailsService.cachedNameDetails(any[Request[_]])).thenReturn(Future.successful(None))
      when(
        mockMatchingService.matchBusiness(
          meq(ValidUtr),
          meq(charityPublicBodyNotForProfitOrganisation),
          meq(None),
          any()
        )(any[Request[AnyContent]], any[HeaderCarrier])
      ).thenReturn(Future.successful(false))
      submitForm(ValidUtrRequest, CdsOrganisationType.CharityPublicBodyNotForProfitId) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(bodyOf(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe BusinessNotMatchedError
        page.getElementsText("title") should startWith("Error: ")
      }
    }
  }

  def showForm(organisationType: String, userId: String = defaultUserId)(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    val result =
      controller.form(organisationType, Journey.GetYourEORI).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitForm(
    form: Map[String, String],
    organisationType: String,
    userId: String = defaultUserId,
    isInReviewMode: Boolean = false
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    val result = controller
      .submit(organisationType, Journey.GetYourEORI, isInReviewMode)
      .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    test(result)
  }
}
