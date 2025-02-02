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

package uk.gov.hmrc.customs.rosmfrontend.controllers.migration

import play.api.Application
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.rosmfrontend.controllers.CdsController
import uk.gov.hmrc.customs.rosmfrontend.controllers.migration.routes.WhatIsYourUtrSubscriptionController
import uk.gov.hmrc.customs.rosmfrontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.customs.rosmfrontend.domain._
import uk.gov.hmrc.customs.rosmfrontend.domain.subscription.UtrSubscriptionFlowPage
import uk.gov.hmrc.customs.rosmfrontend.forms.MatchingForms.yesNoCustomAnswerForm
import uk.gov.hmrc.customs.rosmfrontend.models.Journey
import uk.gov.hmrc.customs.rosmfrontend.services.cache.RequestSessionData
import uk.gov.hmrc.customs.rosmfrontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.customs.rosmfrontend.views.html.migration.match_utr_subscription_yes_no
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HaveUtrSubscriptionYesNoController @Inject()(
  override val currentApp: Application,
  override val authConnector: AuthConnector,
  requestSessionData: RequestSessionData,
  subscriptionFlowManager: SubscriptionFlowManager,
  mcc: MessagesControllerComponents,
  matchUtrSubscriptionYesNoView: match_utr_subscription_yes_no,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(journey: Journey.Value): Action[AnyContent] = ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      requestSessionData.userSelectedOrganisationType match {
        case Some(orgType) => Future.successful(Ok(
          matchUtrSubscriptionYesNoView(yesNoCustomAnswerForm(errorMessageByAnswer(orgType), "have-utr"), orgType.id, journey)))
        case None => noOrgTypeSelected
      }
  }

  def submit(journey: Journey.Value): Action[AnyContent] = ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      requestSessionData.userSelectedOrganisationType match {
        case Some(orgType) =>
          yesNoCustomAnswerForm(errorMessageByAnswer(orgType), "have-utr").bindFromRequest.fold(
            formWithErrors =>
              Future.successful(BadRequest(matchUtrSubscriptionYesNoView(formWithErrors, orgType.id, journey))),
            formData => destinationsByAnswer(formData, journey, orgType)
          )
        case None => noOrgTypeSelected
      }
  }

  private def errorMessageByAnswer(orgType: CdsOrganisationType)(implicit hc: HeaderCarrier, request: Request[AnyContent]): String = {
    orgType match {
      case CdsOrganisationType.Company | CdsOrganisationType.ThirdCountryOrganisation => Messages("cds.matching.row-organisation.utr.error")
      case _ => Messages("cds.matching.row-sole-trader-individual.utr.error")
    }
  }

  private def destinationsByAnswer(form: YesNo, journey: Journey.Value, orgType: CdsOrganisationType)(
    implicit hc: HeaderCarrier,
    request: Request[AnyContent]
  ): Future[Result] =
    if (form.isYes) {
      Future.successful(Redirect(WhatIsYourUtrSubscriptionController.createForm(journey)))
    } else {
      Future.successful(Redirect(subscriptionFlowManager.stepInformation(UtrSubscriptionFlowPage).nextPage.url))
    }

  private lazy val noOrgTypeSelected = throw new IllegalStateException("No organisation type selected by user")
}
