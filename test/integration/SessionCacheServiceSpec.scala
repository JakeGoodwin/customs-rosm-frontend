/*
 * Copyright 2022 HM Revenue & Customs
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

package integration

import common.support.testdata.registration.RegistrationInfoGenerator._
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json.toJson
import play.api.mvc.{Request, Session}
import play.libs.Json
import uk.gov.hmrc.customs.rosmfrontend.config.AppConfig
import uk.gov.hmrc.customs.rosmfrontend.domain._
import uk.gov.hmrc.customs.rosmfrontend.domain.messaging.ResponseCommon
import uk.gov.hmrc.customs.rosmfrontend.domain.subscription.{BusinessShortName, SubscriptionDetails}
import uk.gov.hmrc.customs.rosmfrontend.services.Save4LaterService
import uk.gov.hmrc.customs.rosmfrontend.services.cache.{CachedData, DataUnavailableException, SessionCache}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.test.MongoSupport
import util.builders.RegistrationDetailsBuilder._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SessionCacheSpec extends IntegrationTestsSpec with MockitoSugar with MongoSupport {

  lazy val appConfig = app.injector.instanceOf[AppConfig]
  val mockTimeStampSupport = new CurrentTimestampSupport()

  private val save4LaterService      = app.injector.instanceOf[Save4LaterService]
  implicit val request: Request[Any] = mock[Request[Any]]
  val hc: HeaderCarrier              = mock[HeaderCarrier]
  val sessionCache                   = new SessionCache(appConfig, mongoComponent, save4LaterService, mockTimeStampSupport)


  "Session cache" should {

    "store, fetch and update Subscription details handler correctly" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      val holder = SubscriptionDetails(businessShortName = Some(BusinessShortName(Some("some business name"))))

      await(sessionCache.saveSubscriptionDetails(holder)(request))

      val expectedJson = toJson(CachedData(subDetails = Some(holder)))
      val cache = await(sessionCache.cacheRepo.findById(request))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson

      await(sessionCache.subscriptionDetails(request)) mustBe holder

      val updatedHolder = SubscriptionDetails(
        businessShortName = Some(BusinessShortName(Some("different business name"))),
        sicCode = Some("sic")
      )

      await(sessionCache.saveSubscriptionDetails(updatedHolder)(request))

      val expectedUpdatedJson                   = toJson(CachedData(subDetails = Some(updatedHolder)))
      val updatedCache                          = await(sessionCache.cacheRepo.findById(request))
      val Some(CacheItem(_, updatedJson, _, _)) = updatedCache
      updatedJson mustBe expectedUpdatedJson
    }

    "provide default when subscription details holder not in cache" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      await(sessionCache.putSession(DataKey("regDetails"), data = Json.toJson(individualRegistrationDetails)))

      await(sessionCache.subscriptionDetails(request)) mustBe SubscriptionDetails()
    }

    "store, fetch SubscriptionCreateOutcome correctly" in {
      val sessionId: SessionId = setupSession
      val subscriptionCreateOutcome = SubscriptionCreateOutcome("GB02118283272", "23 June 2018", Some("orgName"))
      await(sessionCache.saveSubscriptionCreateOutcome(subscriptionCreateOutcome)(request))
      val cache = await(sessionCache.cacheRepo.findById(request))
      val expectedJson = toJson(CachedData(subscriptionCreateOutcome = Some(subscriptionCreateOutcome)))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson
      await(sessionCache.subscriptionCreateOutcome(request)) mustBe subscriptionCreateOutcome
    }

    "store, fetch SubscriptionStatusOutcome correctly" in {
      val sessionId: SessionId = setupSession
      val subscriptionStatusOutcome = SubscriptionStatusOutcome("23 June 2018")
      await(sessionCache.saveSubscriptionStatusOutcome(subscriptionStatusOutcome)(request))
      val cache = await(sessionCache.cacheRepo.findById(request))
      val expectedJson = toJson(CachedData(subscriptionStatusOutcome = Some(subscriptionStatusOutcome)))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson
      await(sessionCache.subscriptionStatusOutcome(request)) mustBe subscriptionStatusOutcome

    }
    "store, fetch Email correctly" in {
      val sessionId: SessionId = setupSession
      val email = "test@test.com"
      await(sessionCache.saveEmail(email)(request))
      val cache = await(sessionCache.cacheRepo.findById(request))
      val expectedJson = toJson(CachedData(email = Some(email)))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson
      await(sessionCache.email(request)) mustBe email
      await(sessionCache.mayBeEmail(request)) mustBe Some(email)

    }
    "store, fetch Eori correctly" in {
      val sessionId: SessionId = setupSession
      val eori = Eori("GB0000000000")
      await(sessionCache.saveEori(eori)(request))
      val cache = await(sessionCache.cacheRepo.findById(request))
      val expectedJson = toJson(CachedData(eori = Some(eori.id)))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson
      await(sessionCache.eori(request)) mustBe Some(eori.id)
    }

    "store, fetch and update Registration details with OrgType correctly" in {
      val sessionId: SessionId = setupSession
      when(save4LaterService.saveOrgType(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
      await(
        sessionCache.saveRegistrationDetails(
          organisationRegistrationDetails,
          InternalId("InternalId"),
          Some(CdsOrganisationType.Company)
        )(hc, request)
      )
      val cache = await(sessionCache.cacheRepo.findById(request))
      val expectedJson = toJson(CachedData(regDetails = Some(organisationRegistrationDetails)))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson
      await(sessionCache.name(request)) mustBe Some(organisationRegistrationDetails.name)

    }

    "store, fetch and update Registration details with OrgType AND SafeId for ROW correctly" in {
      val sessionId: SessionId = setupSession
      when(save4LaterService.saveOrgType(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
      when(save4LaterService.saveSafeId(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
      await(
        sessionCache.saveRegistrationDetailsWithoutId(
          organisationRegistrationDetails,
          InternalId("InternalId"),
          Some(CdsOrganisationType.Company)
        )(hc, request)
      )
      val cache = await(sessionCache.cacheRepo.findById(request))
      val expectedJson = toJson(CachedData(regDetails = Some(organisationRegistrationDetails)))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson
    }

    "store, fetch and update Registration details correctly" in {
      await(sessionCache.saveRegistrationDetails(organisationRegistrationDetails)(request))

      val cache = await(sessionCache.cacheRepo.findById(request))

      val expectedJson                   = toJson(CachedData(regDetails = Some(organisationRegistrationDetails)))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson

      await(sessionCache.registrationDetails(request)) mustBe organisationRegistrationDetails
      await(sessionCache.saveRegistrationDetails(individualRegistrationDetails)(request))

      val updatedCache = await(sessionCache.cacheRepo.findById(request))

      val expectedUpdatedJson                   = toJson(CachedData(regDetails = Some(individualRegistrationDetails)))
      val Some(CacheItem(_, updatedJson, _, _)) = updatedCache
      updatedJson mustBe expectedUpdatedJson
    }

    "store and fetch RegisterWith EORI And Id Response correctly for Register with Eori and Id response" in {
      val sessionId: SessionId = setupSession

      val processingDate = DateTime.now.withTimeAtStartOfDay()
      val responseCommon = ResponseCommon(status = "OK", processingDate = processingDate)
      val trader = Trader(fullName = "New trading", shortName = "nt")
      val establishmentAddress =
        EstablishmentAddress(streetAndNumber = "new street", city = "leeds", countryCode = "GB")
      val responseData: ResponseData = ResponseData(
        SAFEID = "SomeSafeId",
        trader = trader,
        establishmentAddress = establishmentAddress,
        hasInternetPublication = true,
        startDate = "2018-01-01"
      )
      val registerWithEoriAndIdResponseDetail = RegisterWithEoriAndIdResponseDetail(
        outcome = Some("PASS"),
        caseNumber = Some("case no 1"),
        responseData = Some(responseData)
      )
      val rd = RegisterWithEoriAndIdResponse(
        responseCommon = responseCommon,
        responseDetail = Some(registerWithEoriAndIdResponseDetail)
      )

      await(sessionCache.saveRegisterWithEoriAndIdResponse(rd)(request))

      val cache = await(sessionCache.cacheRepo.findById(request))

      val expectedJson = toJson(CachedData(registerWithEoriAndIdResponse = Some(rd)))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson

      await(sessionCache.registerWithEoriAndIdResponse(request)) mustBe rd
    }

    "throw exception when registration Details requested and not available in cache" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-123"))))
      await(sessionCache.putSession(DataKey("sub01Outcome"), data = Json.toJson(CacheItem(_, _, _, _))))

      val caught = intercept[DataUnavailableException] {
        await(sessionCache.registrationDetails(request))
      }
      caught.getMessage mustBe s"regDetails is not cached in data for the sessionId: sessionId-123"
    }

    "store, fetch and update Registration Info correctly" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      await(sessionCache.saveRegistrationInfo(organisationRegistrationInfoWithAllOptionalValues)(request))

      val cache = await(sessionCache.cacheRepo.findById(request))

      val expectedJson                   = toJson(CachedData(regInfo = Some(organisationRegistrationInfoWithAllOptionalValues)))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson

      await(sessionCache.registrationInfo(request)) mustBe organisationRegistrationInfoWithAllOptionalValues
      await(sessionCache.saveRegistrationInfo(individualRegistrationInfoWithAllOptionalValues)(request))

      val updatedCache = await(sessionCache.cacheRepo.findById(request))

      val expectedUpdatedJson                   = toJson(CachedData(regInfo = Some(individualRegistrationInfoWithAllOptionalValues)))
      val Some(CacheItem(_, updatedJson, _, _)) = updatedCache
      updatedJson mustBe expectedUpdatedJson
    }

    "throw exception when registration info requested and not available in cache" in {
      val s: SessionId = setupSession
      when(request.session).thenReturn(Session(Map(("sessionId", s.value))))

      val caught = intercept[DataUnavailableException] {
        await(sessionCache.registrationInfo(request))
      }
      caught.getMessage mustBe s"regInfo is not cached in data for the sessionId: ${s.value}"
    }

    "store Registration Details, Info and Subscription Details Holder correctly" in {
      val sessionId: SessionId = setupSession

      await(sessionCache.saveRegistrationDetails(organisationRegistrationDetails)(request))
      val holder = SubscriptionDetails()
      await(sessionCache.saveSubscriptionDetails(holder)(request))
      await(sessionCache.saveRegistrationInfo(organisationRegistrationInfoWithAllOptionalValues)(request))
      val cache = await(sessionCache.cacheRepo.findById(request))

      val expectedJson = toJson(
        CachedData(
          Some(organisationRegistrationDetails),
          Some(holder),
          Some(organisationRegistrationInfoWithAllOptionalValues)
          )
        )

      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson
    }

    "remove from the cache" in {
      val sessionId: SessionId = setupSession
      await(sessionCache.saveRegistrationDetails(organisationRegistrationDetails)(request))

      await(sessionCache.remove(request))

      val cached = await(sessionCache.cacheRepo.findById(request))
      cached mustBe None
    }
  }

  private def setupSession: SessionId = {
    val sessionId = SessionId("sessionId-" + UUID.randomUUID())
    when(hc.sessionId).thenReturn(Some(sessionId))
    sessionId
  }
}
