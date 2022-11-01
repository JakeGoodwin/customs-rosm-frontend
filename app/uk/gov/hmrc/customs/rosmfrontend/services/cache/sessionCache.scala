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

package uk.gov.hmrc.customs.rosmfrontend.services.cache

import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc.Request
import uk.gov.hmrc.customs.rosmfrontend.config.AppConfig
import uk.gov.hmrc.customs.rosmfrontend.domain._
import uk.gov.hmrc.customs.rosmfrontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.customs.rosmfrontend.services.Save4LaterService
import uk.gov.hmrc.customs.rosmfrontend.services.cache.CachedData._
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.mongo.cache.{DataKey, SessionCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.play.http.logging.Mdc.preservingMdc

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.{NoStackTrace, NonFatal}

sealed case class CachedData(
  regDetails: Option[RegistrationDetails] = None,
  subDetails: Option[SubscriptionDetails] = None,
  regInfo: Option[RegistrationInfo] = None,
  subscriptionCreateOutcome: Option[SubscriptionCreateOutcome] = None,
  subscriptionStatusOutcome: Option[SubscriptionStatusOutcome] = None,
  registerWithEoriAndIdResponse: Option[RegisterWithEoriAndIdResponse] = None,
  email: Option[String] = None,
  eori: Option[String] = None,
  hasNino: Option[Boolean] = None)

object CachedData {
  val regDetailsKey = "regDetails"
  val regInfoKey = "regInfo"
  val subDetailsKey = "subDetails"
  val subscriptionStatusOutcomeKey = "subscriptionStatusOutcome"
  val subscriptionCreateOutcomeKey = "subscriptionCreateOutcome"
  val registerWithEoriAndIdResponseKey = "registerWithEoriAndIdResponse"
  val emailKey = "email"
  val hasNinoKey = "hasNino"
  val safeIdKey = "safeId"
  val groupIdKey = "cachedGroupId"
  val eoriKey = "eori"
  implicit val format = Json.format[CachedData]
}

@Singleton
class SessionCache @Inject() (
   appConfig: AppConfig,
   mongo: MongoComponent,
   save4LaterService: Save4LaterService,
   timestampSupport: TimestampSupport
)(implicit ec: ExecutionContext)
    extends SessionCacheRepository(
      mongo,
      "session-cache",
      ttl = appConfig.ttl,
      timestampSupport = timestampSupport,
      sessionIdKey = SessionKeys.sessionId
      )(ec){

  private def sessionId(implicit request: Request[_]): String =
    request.session.get("sessionId") match {
      case None =>
        // $COVERAGE-OFF$
        throw new IllegalStateException("Session id is not available")
      // $COVERAGE-ON$
      case Some(sessionId) =>  sessionId
    }

  def putData[A: Writes](key: String, data: A)(implicit request: Request[_]): Future[A] =
    preservingMdc {
      putSession[A](DataKey(key), data).map(_ => data)
    }

  def getData[A: Reads](key: String)(implicit request: Request[_]): Future[Option[A]] =
    preservingMdc {
      getFromSession[A](DataKey(key))
    }


  def saveRegistrationDetails(rd: RegistrationDetails)(implicit request: Request[_]): Future[Boolean] =
    putData(regDetailsKey, Json.toJson(rd)) map (_ => true)

  def saveRegistrationDetails(
    rd: RegistrationDetails,
    internalId: InternalId,
    orgType: Option[CdsOrganisationType] = None
  )(implicit headerCarrier: HeaderCarrier, request: Request[_]): Future[Boolean] =
    for {
      _ <- save4LaterService.saveOrgType(internalId, orgType)
      createdOrUpdated <- saveRegistrationDetails(rd)
    } yield createdOrUpdated
  //ROW
  def saveRegistrationDetailsWithoutId(
    rd: RegistrationDetails,
    internalId: InternalId,
    orgType: Option[CdsOrganisationType] = None
  )(implicit  headerCarrier: HeaderCarrier, request: Request[_]): Future[Boolean] =
    for {
      _ <- save4LaterService.saveSafeId(internalId, rd.safeId)
      _ <- save4LaterService.saveOrgType(internalId, orgType)
      createdOrUpdated <- saveRegistrationDetails(rd)
    } yield createdOrUpdated

  def saveRegisterWithEoriAndIdResponse(
    rd: RegisterWithEoriAndIdResponse
  )(implicit request: Request[_]): Future[Boolean] =
    putData(registerWithEoriAndIdResponseKey, Json.toJson(rd)) map (_ => true)

  def saveSubscriptionCreateOutcome(subscribeOutcome: SubscriptionCreateOutcome)(implicit request: Request[_]): Future[Boolean] =
    putData(subscriptionCreateOutcomeKey, Json.toJson(subscribeOutcome)) map (_ => true)

  def saveSubscriptionStatusOutcome(subscriptionStatusOutcome: SubscriptionStatusOutcome)(implicit request: Request[_]): Future[Boolean] =
    putData(subscriptionStatusOutcomeKey, Json.toJson(subscriptionStatusOutcome)) map (_ => true)

  def saveRegistrationInfo(rd: RegistrationInfo)(implicit request: Request[_]): Future[Boolean] =
    putData(regInfoKey, Json.toJson(rd)) map (_ => true)

  def saveSubscriptionDetails(rdh: SubscriptionDetails)(implicit request: Request[_]): Future[Boolean] =
    putData(subDetailsKey, Json.toJson(rdh)) map (_ => true)

  def saveEmail(email: String)(implicit request: Request[_]): Future[Boolean] =
    putData(emailKey, Json.toJson(email)) map (_ => true)

  def saveHasNino(hasNino: Boolean)(implicit request: Request[_]): Future[Boolean] =
    putData(hasNinoKey, Json.toJson(hasNino)) map (_ => true)

  def saveEori(eori: Eori)(implicit request: Request[_]): Future[Boolean] =
    putData(eoriKey, Json.toJson(eori.id)) map (_ => true)

  def subscriptionDetails(implicit request: Request[_]): Future[SubscriptionDetails] =
    getData[SubscriptionDetails](subDetailsKey).map(_.getOrElse(SubscriptionDetails()))

  def eori(implicit request: Request[_]): Future[Option[String]] =
    getData[String](eoriKey)

  def email(implicit request: Request[_]): Future[String] =
    getData[String](emailKey).map(_.getOrElse(throwException(emailKey)))

  def hasNino(implicit request: Request[_]): Future[Option[Boolean]] =
    getData[Boolean](hasNinoKey) // .map(_.getOrElse(throwException(hasNinoKey)))

  def mayBeEmail(implicit request: Request[_]): Future[Option[String]] =
    getData[String](emailKey)

  def safeId(implicit request: Request[_]): Future[SafeId] = fetchSafeIdFromRegDetails.flatMap {
    case Some(value) => Future.successful(value)
    case None =>
      fetchSafeIdFromReg06Response.map(
        _.getOrElse(throw new IllegalStateException(s"$safeIdKey is not cached in data for the sessionId: $sessionId"))
        )
  }

  def fetchSafeIdFromReg06Response(implicit request: Request[_]): Future[Option[SafeId]] =
    registerWithEoriAndIdResponse.map(
      response =>
        response.responseDetail.flatMap(_.responseData.map(_.SAFEID))
                .map(SafeId(_))
      ).recoverWith {
      case NonFatal(_) => Future.successful(None)
    }

  def fetchSafeIdFromRegDetails(implicit request: Request[_]): Future[Option[SafeId]] =
    registrationDetails.map(response => if (response.safeId.id.nonEmpty) Some(response.safeId) else None)
                       .recoverWith {
                         case NonFatal(_) => Future.successful(None)
                       }

  def name(implicit request: Request[_]): Future[Option[String]] =
    getData[RegistrationDetails](regDetailsKey).map(_.map(_.name))

  def registrationDetails(implicit request: Request[_]): Future[RegistrationDetails] =
    getData[RegistrationDetails](regDetailsKey).map(_.getOrElse(throwException(regDetailsKey)))

  def registerWithEoriAndIdResponse(implicit request: Request[_]): Future[RegisterWithEoriAndIdResponse] =
    getData[RegisterWithEoriAndIdResponse](registerWithEoriAndIdResponseKey)
      .map(_.getOrElse(throwException(registerWithEoriAndIdResponseKey)))

  def subscriptionStatusOutcome(implicit request: Request[_]): Future[SubscriptionStatusOutcome] =
    getData[SubscriptionStatusOutcome](subscriptionStatusOutcomeKey)
      .map(_.getOrElse(throwException(subscriptionStatusOutcomeKey)))

  def subscriptionCreateOutcome(implicit request: Request[_]): Future[SubscriptionCreateOutcome] =
    getData[SubscriptionCreateOutcome](subscriptionCreateOutcomeKey)
      .map(_.getOrElse(throwException(subscriptionCreateOutcomeKey)))

  def mayBeSubscriptionCreateOutcome(implicit request: Request[_]): Future[Option[SubscriptionCreateOutcome]] =
    getData[SubscriptionCreateOutcome](subscriptionCreateOutcomeKey)

  def registrationInfo(implicit request: Request[_]): Future[RegistrationInfo] =
    getData[RegistrationInfo](regInfoKey).map(_.getOrElse(throwException(regInfoKey)))

  def remove(implicit request: Request[_]): Future[Boolean] =
    preservingMdc {
      cacheRepo.deleteEntity(request).map(_ => true).recoverWith {
        case _ => Future.successful(false)
      }
    }

  private def throwException(name: String)(implicit request: Request[_]) =
    throw DataUnavailableException(s"$name is not cached in data for the sessionId: $sessionId")
}

case class SessionTimeOutException(errorMessage: String) extends NoStackTrace
case class DataUnavailableException(message: String)     extends RuntimeException(message)
