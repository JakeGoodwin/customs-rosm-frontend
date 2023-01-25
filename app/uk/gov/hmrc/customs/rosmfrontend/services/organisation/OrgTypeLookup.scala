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

package uk.gov.hmrc.customs.rosmfrontend.services.organisation

import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.rosmfrontend.domain.{EtmpOrganisationType, RegistrationDetailsOrganisation}
import uk.gov.hmrc.customs.rosmfrontend.services.cache.{RequestSessionData, SessionCache}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class OrgTypeLookup @Inject()(requestSessionData: RequestSessionData, sessionCache: SessionCache) {

  def etmpOrgType(implicit request: Request[AnyContent]): Future[Option[EtmpOrganisationType]] =
    requestSessionData.userSelectedOrganisationType match {
      case Some(cdsOrgType) => Future.successful(Some(EtmpOrganisationType(cdsOrgType)))
      case None =>
        sessionCache.registrationDetails map {
          case RegistrationDetailsOrganisation(_, _, _, _, _, _, orgType) => orgType
          case _                                                          => throw new IllegalStateException("No Registration details in cache.")
        }
    }
}
