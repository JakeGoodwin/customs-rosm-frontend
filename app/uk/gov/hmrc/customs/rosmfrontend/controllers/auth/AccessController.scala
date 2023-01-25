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

package uk.gov.hmrc.customs.rosmfrontend.controllers.auth

import play.api.i18n.I18nSupport
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole, User}
import uk.gov.hmrc.customs.rosmfrontend.controllers.{JourneyTypeFromUrl, routes}
import uk.gov.hmrc.customs.rosmfrontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.customs.rosmfrontend.models.Journey

import scala.concurrent.Future

trait AccessController extends JourneyTypeFromUrl with AllowlistVerification with EnrolmentExtractor with I18nSupport {

  /*
  * Current check order based on https://jira.tools.tax.service.gov.uk/browse/ECC-981:
  * 1. Check User type (Agent check) & email check
  * 2. Check enrolment
  * 3. Organisation Cred Role Assistant check
  * */
  def permitUserOrRedirect(
    loggedInUser: LoggedInUserWithEnrolments,
    affinityGroup: Option[AffinityGroup],
    credentialRole: Option[CredentialRole],
    email: Option[String]
  )(action: => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    val journey = if (request.path.contains("register-for-cds")) Journey.GetYourEORI else Journey.Migrate

    (isPermittedUserType(affinityGroup), isPermitted(email)) match {
      case (true, true) =>
        if (isEnrolled(loggedInUser)) {
          Future.successful(Redirect(routes.EnrolmentAlreadyExistsController.enrolmentAlreadyExists(journey)))
        } else {
          if (isPermittedCredentialRole(credentialRole))
            action
          else
            Future.successful(Redirect(routes.YouCannotUseServiceController.page(journeyFromUrl)))
        }
      case (true, false) => Future.successful(Redirect(routes.YouCannotUseServiceController.unauthorisedPage()))
      case (false, _) => Future.successful(Redirect(routes.YouCannotUseServiceController.page(journeyFromUrl)))
    }
  }

  private def isEnrolled(loggedInUser: LoggedInUserWithEnrolments) = {
    enrolledEori(loggedInUser) match {
      case Some(_) => true
      case None => false
    }
  }

  private def isPermitted(email: Option[String])(implicit request: Request[AnyContent]): Boolean =
    journeyFromUrl == Journey.GetYourEORI || isAllowlisted(email)

  private def isPermittedUserType(affinityGroup: Option[AffinityGroup]): Boolean =
    affinityGroup match {
      case Some(Agent) => false
      case _ => true
    }

  private def isPermittedCredentialRole(credentialRole: Option[CredentialRole]): Boolean =
    credentialRole match {
      case Some(User) => true
      case _ => false
    }
}
