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

package unit.services.cache

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Request
import uk.gov.hmrc.customs.rosmfrontend.domain.LoggedInUser
import uk.gov.hmrc.customs.rosmfrontend.services.cache.{ClearCacheAndRegistrationIdentificationService, SessionCache}
import uk.gov.hmrc.http.HeaderCarrier
import util.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClearCacheAndRegistrationIdentificationServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterAll {
  val mockSessionCache: SessionCache = mock[SessionCache]
  val loggedInUserId = "user-id"
  val Failure = new RuntimeException("something bad has happened")

  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]
  val mockLoggedInUser: LoggedInUser = mock[LoggedInUser]

  override protected def beforeAll() {
    when(mockLoggedInUser.userId()).thenReturn(loggedInUserId)
  }

  val service = new ClearCacheAndRegistrationIdentificationService(mockSessionCache)

  "ClearCacheAndRegistrationIdentificationService" should {
    "clear cache and database" in {
      when(mockSessionCache.saveEmail(any())(any())).thenReturn(Future.successful(true))
      when(mockSessionCache.email(any[Request[_]])).thenReturn(Future.successful("testEmail"))
      when(mockSessionCache.remove(any[Request[_]])).thenReturn(Future.successful(true))

      await(service.clear(any[Request[_]])) should be(())
    }

    "return a failure if cache clear fails unexpectedly" in {
      when(mockSessionCache.remove(any[Request[_]])).thenReturn(Future.failed(Failure))

      intercept[RuntimeException] {
        await(service.clear(any[Request[_]]))
      } shouldBe Failure
    }
  }
}
