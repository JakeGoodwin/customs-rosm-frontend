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

import org.joda.time.{DateTime, DateTimeZone}
import uk.gov.hmrc.customs.rosmfrontend.domain.messaging.{RequestCommon, RequestParameter}

import java.time.Clock
import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class RequestCommonGenerator @Inject()(uuidGenerator: RandomUUIDGenerator, clock: UtcClock) {

  def generate(requestParameters: Option[Seq[RequestParameter]] = None): RequestCommon =
    RequestCommon(
      regime = "CDS",
      receiptDate = receiptDate,
      acknowledgementReference = uuidGenerator.generateUUIDAsString,
      requestParameters = requestParameters
    )

  def receiptDate: DateTime =
    new DateTime(clock.generateUtcTime.instant.toEpochMilli, DateTimeZone.UTC)
}

@Singleton
class RandomUUIDGenerator {
  def generateUUIDAsString: String = UUID.randomUUID().toString.replace("-", "")
}

@Singleton
class UtcClock {
  def generateUtcTime: Clock = Clock.systemUTC()
}
