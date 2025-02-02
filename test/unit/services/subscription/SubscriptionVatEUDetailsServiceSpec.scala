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

package unit.services.subscription

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Request
import uk.gov.hmrc.customs.rosmfrontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.customs.rosmfrontend.forms.models.subscription.VatEUDetailsModel
import uk.gov.hmrc.customs.rosmfrontend.services.subscription.{SubscriptionBusinessService, SubscriptionDetailsService, SubscriptionVatEUDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import util.UnitSpec

import scala.concurrent.Future

class SubscriptionVatEUDetailsServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  implicit val request: Request[_] = mock[Request[_]]

  private val mockSubscriptionDetails = mock[SubscriptionDetails]
  private val mockVatEuDetails = mock[VatEUDetailsModel]
  private val previouslyCachedSubscriptionDetailsHolder = SubscriptionDetails(vatEUDetails = Seq(mockVatEuDetails))

  private val VatEuDetailsForUpdate = Seq(VatEUDetailsModel("12345", "FR"), VatEUDetailsModel("54321", "DE"))

  private val exampleSubscriptionDetails = SubscriptionDetails(vatEUDetails = VatEuDetailsForUpdate)

  private val emulatedFailure = new RuntimeException("Something went wrong!")

  private val service =
    new SubscriptionVatEUDetailsService(mockSubscriptionBusinessService, mockSubscriptionDetailsService)

  override protected def beforeEach() {
    reset(mockSubscriptionBusinessService, mockSubscriptionDetails, mockSubscriptionDetailsService)
    when(mockSubscriptionBusinessService.retrieveSubscriptionDetailsHolder(meq(request)))
      .thenReturn(previouslyCachedSubscriptionDetailsHolder)
  }

  "VAT EU Details caching" should {
    "save VAT EU Details for the first time" in {

      when(
        mockSubscriptionDetailsService
          .saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(meq(request))
      ).thenReturn(Future.successful[Unit](()))

      await(service.saveOrUpdate(mockVatEuDetails)) should be(())

      verify(mockSubscriptionBusinessService).retrieveSubscriptionDetailsHolder(meq(request))
      verify(mockSubscriptionDetailsService).saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(
        meq(request)
      )
    }

    "update EU Details in previously cached SubscriptionDetailsHolder" in {
      when(
        mockSubscriptionDetailsService
          .saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(meq(request))
      ).thenReturn(Future.successful[Unit](()))

      await(service.saveOrUpdate(mockVatEuDetails)) should be(())

      verify(mockSubscriptionBusinessService).retrieveSubscriptionDetailsHolder(meq(request))
      verify(mockSubscriptionDetailsService).saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(
        meq(request)
      )
    }

    "fail when cache fails accessing current SubscriptionDetailsHolder" in {
      when(mockSubscriptionBusinessService.retrieveSubscriptionDetailsHolder(meq(request)))
        .thenReturn(Future.failed(emulatedFailure))

      intercept[RuntimeException] {
        await(service.saveOrUpdate(mockVatEuDetails))
      } shouldBe emulatedFailure

      verify(mockSubscriptionBusinessService).retrieveSubscriptionDetailsHolder(meq(request))
      verifyNoMoreInteractions(mockSubscriptionBusinessService)
    }

    "update EU Details in previously cached SubscriptionDetailsHolder using sequence" in {
      when(
        mockSubscriptionDetailsService
          .saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(meq(request))
      ).thenReturn(Future.successful[Unit](()))

      await(service.saveOrUpdate(Seq(mockVatEuDetails))) should be(())

      verify(mockSubscriptionBusinessService).retrieveSubscriptionDetailsHolder(meq(request))
      verify(mockSubscriptionDetailsService).saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(
        meq(request)
      )
    }

    "fail when cache fails accessing current SubscriptionDetailsHolder using sequence" in {
      when(mockSubscriptionBusinessService.retrieveSubscriptionDetailsHolder(meq(request)))
        .thenReturn(Future.failed(emulatedFailure))

      intercept[RuntimeException] {
        await(service.saveOrUpdate(Seq(mockVatEuDetails)))
      } shouldBe emulatedFailure

      verify(mockSubscriptionBusinessService).retrieveSubscriptionDetailsHolder(meq(request))
      verifyNoMoreInteractions(mockSubscriptionBusinessService)
    }
  }

  "VAT EU Details retrieve from cache" should {
    "give Nil when cached SubscriptionDetailsHolder does not hold VAT EU Details" in {
      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(request))).thenReturn(Future.successful(Seq()))
      await(service.cachedEUVatDetails) shouldBe Nil
      verify(mockSubscriptionBusinessService).getCachedVatEuDetailsModel(meq(request))
    }

    "give Some previously cached VAT Identifications" in {
      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(request)))
        .thenReturn(Future.successful(Seq(mockVatEuDetails)))
      await(service.cachedEUVatDetails) shouldBe List(mockVatEuDetails)

      verify(mockSubscriptionBusinessService).getCachedVatEuDetailsModel(meq(request))
    }

    "fail when cache fails accessing current SubscriptionDetailsHolder" in {
      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(request)))
        .thenReturn(Future.failed(emulatedFailure))

      intercept[RuntimeException] {
        await(service.cachedEUVatDetails)
      } shouldBe emulatedFailure

      verify(mockSubscriptionBusinessService).getCachedVatEuDetailsModel(meq(request))
      verifyNoMoreInteractions(mockSubscriptionBusinessService)
    }
  }

  "Updating single vat details" should {
    "return subscription details's vat eu details updated" in {
      val vatEuUpdate = VatEUDetailsModel("54321", "ES")

      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(request)))
        .thenReturn(Future.successful(VatEuDetailsForUpdate))
      await(service.updateVatEuDetailsModel(VatEuDetailsForUpdate.head, vatEuUpdate)) shouldBe Seq(
        VatEUDetailsModel("54321", "ES"),
        VatEUDetailsModel("54321", "DE")
      )
    }

    "fails when details for update not found in cache" in {
      val vatEuUpdate = VatEUDetailsModel("54321", "ES")
      val nonExistingVatReference = VatEUDetailsModel("12345", "PL")

      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(request)))
        .thenReturn(Future.successful(VatEuDetailsForUpdate))

      intercept[IllegalArgumentException] {
        await(service.updateVatEuDetailsModel(nonExistingVatReference, vatEuUpdate))
      } getMessage () shouldBe "Details for update do not exist in a cache"

      verify(mockSubscriptionBusinessService).getCachedVatEuDetailsModel(meq(request))
      verifyNoMoreInteractions(mockSubscriptionBusinessService)
    }
  }

  "Querying for specific vatEuDetails" should {
    "return vatEuDetails when index was found" in {
      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(request)))
        .thenReturn(Future.successful(VatEuDetailsForUpdate))
      await(service.vatEuDetails(VatEuDetailsForUpdate.head.index)) shouldBe Some(VatEUDetailsModel("12345", "FR"))
    }

    "return None when index not found" in {
      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(request)))
        .thenReturn(Future.successful(VatEuDetailsForUpdate))
      await(service.vatEuDetails(VatEuDetailsForUpdate.size)) shouldBe None
    }
  }

  "Asking for cached Eu Vat Details" should {
    "should call subscription business service " in {
      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(request)))
        .thenReturn(Future.successful(Seq(mockVatEuDetails)))
      await(service.vatEuDetails(VatEuDetailsForUpdate.size))
      verify(mockSubscriptionBusinessService).getCachedVatEuDetailsModel(meq(request))
    }
  }

  "Removing single vat eu details" should {
    "call SubscriptionDetailsService to save filtered vatEuDetails when value to be removed was found" in {
      def sub = (subDet: SubscriptionDetails) => subDet.copy(vatEUDetails = Seq(VatEUDetailsModel("54321", "DE")))

      when(mockSubscriptionBusinessService.getCachedVatEuDetailsModel(meq(request)))
        .thenReturn(Future.successful(VatEuDetailsForUpdate))
      when(
        mockSubscriptionDetailsService
          .saveSubscriptionDetails(any[SubscriptionDetails => SubscriptionDetails]())(meq(request))
      ).thenReturn(Future.successful(()))

      await(service.removeSingleEuVatDetails(VatEUDetailsModel("12345", "FR")))

      val requestCaptor = ArgumentCaptor.forClass(classOf[SubscriptionDetails => SubscriptionDetails])

      verify(mockSubscriptionDetailsService).saveSubscriptionDetails(requestCaptor.capture())(ArgumentMatchers.eq(request))
      val f = requestCaptor.getValue.asInstanceOf[SubscriptionDetails => SubscriptionDetails]

      f(exampleSubscriptionDetails) should equal(sub(exampleSubscriptionDetails))
    }
  }
}
