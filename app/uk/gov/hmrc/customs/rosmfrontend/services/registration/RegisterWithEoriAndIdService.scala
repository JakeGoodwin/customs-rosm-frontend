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

package uk.gov.hmrc.customs.rosmfrontend.services.registration

import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.rosmfrontend.connector.RegisterWithEoriAndIdConnector
import uk.gov.hmrc.customs.rosmfrontend.domain._
import uk.gov.hmrc.customs.rosmfrontend.domain.messaging.Individual
import uk.gov.hmrc.customs.rosmfrontend.domain.messaging.RegistrationInfoRequest.{NINO, UTR}
import uk.gov.hmrc.customs.rosmfrontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.customs.rosmfrontend.forms.models.subscription.AddressViewModel
import uk.gov.hmrc.customs.rosmfrontend.services.RequestCommonGenerator
import uk.gov.hmrc.customs.rosmfrontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.customs.rosmfrontend.services.mapping.{CdsToEtmpOrganisationType, OrganisationTypeConfiguration}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class RegisterWithEoriAndIdService @Inject()(
  connector: RegisterWithEoriAndIdConnector,
  reqCommonGenerator: RequestCommonGenerator,
  dataCache: SessionCache,
  requestSessionData: RequestSessionData
) {

  def sendIndividualRequest(
    implicit request: Request[AnyContent],
    loggedInUser: LoggedInUser,
    headerCarrier: HeaderCarrier
  ): Future[Boolean] = {
    def ninoOrUtr(id: CustomsId) = id match {
      case _: Nino => NINO
      case _: Utr  => UTR
      case unexpected =>
        throw new IllegalArgumentException("Expected only nino or utr to be populated but got: " + unexpected)
    }

    def createRegDetail(address: AddressViewModel, nameDob: NameDobMatchModel, eori: String, id: CustomsId) = {
      val registerModeEori = RegisterModeEori(
        eori,
        nameDob.name,
        EstablishmentAddress(address.street, address.city, address.postcode, address.countryCode)
      )
      val registerModeId = RegisterModeId(
        ninoOrUtr(id),
        id.id,
        isNameMatched = true,
        Some(Individual(nameDob.firstName, None, nameDob.lastName, nameDob.dateOfBirth.toString)),
        None
      )
      RegisterWithEoriAndIdDetail(registerModeEori, registerModeId, None)
    }

    dataCache.subscriptionDetails.flatMap { subscription =>
      val organisationType = requestSessionData.userSelectedOrganisationType.getOrElse(
        throw new IllegalStateException("Org type missing from cache")
      )
      val maybeOrganisationTypeConfiguration: Option[OrganisationTypeConfiguration] =
        CdsToEtmpOrganisationType(Some(organisationType))
      val address =
        subscription.addressDetails.getOrElse(throw new IllegalStateException("Address missing from subscription"))
      val nameDob =
        subscription.nameDobDetails.getOrElse(throw new IllegalStateException("Name / DOB missing from subscription"))
      val eori =
        subscription.eoriNumber.getOrElse(throw new IllegalStateException("EORI number missing from subscription"))
      val id = subscription.customsId.getOrElse(throw new IllegalStateException("Customs ID missing from subscription"))
      registerWithEoriAndId(
        createRegDetail(address, nameDob, eori, id),
        subscription,
        maybeOrganisationTypeConfiguration
      )
    }
  }

  def sendOrganisationRequest(
    implicit request: Request[AnyContent],
    loggedInUser: LoggedInUser,
    headerCarrier: HeaderCarrier
  ): Future[Boolean] = {
    def regModeId(idType: String, id: String, organisationType: CdsOrganisationType, orgName: String, eori: String) =
      RegisterModeId(
        idType,
        id,
        isNameMatched = true,
        None,
        Some(RegisterWithEoriAndIdOrganisation(orgName, EtmpOrganisationType(organisationType).etmpOrgTypeCode))
      )

    def regModeEORI(address: AddressViewModel, eori: String, orgName: String) =
      RegisterModeEori(
        eori,
        orgName,
        EstablishmentAddress(address.street, address.city, address.postcode.filter(_.nonEmpty), address.countryCode)
      )

    for {
      subscriptionDetails <- dataCache.subscriptionDetails
      organisationType = requestSessionData.userSelectedOrganisationType.getOrElse(
        throw new IllegalStateException("Org type missing from cache")
      )
      maybeOrganisationTypeConfiguration: Option[OrganisationTypeConfiguration] = CdsToEtmpOrganisationType(
        Some(organisationType)
      )
      address = subscriptionDetails.addressDetails.getOrElse(
        throw new IllegalStateException("Address missing from subscription")
      )
      eori = subscriptionDetails.eoriNumber.getOrElse(
        throw new IllegalStateException("EORI number missing from subscription")
      )
      orgDetails = subscriptionDetails.nameIdOrganisationDetails.getOrElse(
        throw new IllegalStateException("Organisation details missing from subscription")
      )
      regEoriAndId = RegisterWithEoriAndIdDetail(
        regModeEORI(address, eori, orgDetails.name),
        regModeId(UTR, orgDetails.id, organisationType, orgDetails.name, eori),
        None
      )
      result <- registerWithEoriAndId(regEoriAndId, subscriptionDetails, maybeOrganisationTypeConfiguration)
    } yield result
  }

  def registerWithEoriAndId(
    value: RegisterWithEoriAndIdDetail,
    subscriptionDetails: SubscriptionDetails,
    maybeOrganisationTypeConfiguration: Option[OrganisationTypeConfiguration]
  )(implicit hc: HeaderCarrier, request: Request[_], loggedInUser: LoggedInUser): Future[Boolean] = {

    def stripKFromUtr: RegisterWithEoriAndIdDetail => RegisterWithEoriAndIdDetail = {
      case r @ RegisterWithEoriAndIdDetail(_, id, _) if id.IDType == UTR =>
        r.copy(registerModeID = id.copy(IDNumber = id.IDNumber.stripSuffix("K").stripSuffix("k")))
      case nonUtr => nonUtr
    }

    def save(
      details: RegisterWithEoriAndIdResponse,
      subscriptionDetails: SubscriptionDetails
    )(implicit request: Request[_], loggedInUserId: LoggedInUser): Future[Boolean] =
      if (details.isResponseData) {
        (details.isDoE, details.isPersonType) match {
          case (true, true) => dataCache.saveRegisterWithEoriAndIdResponse(details)
          case (false, true) => {
            val dateOfEstablishment
              : Option[String] = subscriptionDetails.dateEstablished.map(_.toString()) orElse subscriptionDetails.nameDobDetails
              .map(_.dateOfBirth.toString())
            val detailsWithDateOfEstablishment = details
              .withDateOfEstablishment(
                dateOfEstablishment.getOrElse(throw new IllegalStateException("DOE is missing from Register with Eori and Id response"))
              )
            dataCache.saveRegisterWithEoriAndIdResponse(detailsWithDateOfEstablishment)
          }
          case (true, false) =>
            val typeOfPerson = maybeOrganisationTypeConfiguration.map(_.typeOfPerson)
            val detailsWithTypeOfPerson = details
              .withPersonType(
                typeOfPerson.getOrElse(throw new IllegalStateException("TypeOfPerson is missing from Register with Eori and Id response"))
              )
            dataCache.saveRegisterWithEoriAndIdResponse(detailsWithTypeOfPerson)
          case (false, false) => {
            val date = subscriptionDetails.dateEstablished.map(_.toString()) orElse subscriptionDetails.nameDobDetails
              .map(_.dateOfBirth.toString())
            val typeOfPerson = maybeOrganisationTypeConfiguration.map(_.typeOfPerson)
            val detailsWithDOEAndPersonType: RegisterWithEoriAndIdResponse = details
              .withDateOfEstablishment(
                date.getOrElse(throw new IllegalStateException("DOE is missing from Register with Eori and Id response"))
              )
              .withPersonType(
                typeOfPerson.getOrElse(throw new IllegalStateException("TypeOfPerson is missing from Register with Eori and Id response"))
              )
            dataCache.saveRegisterWithEoriAndIdResponse(detailsWithDOEAndPersonType)
          }
        }
      } else {
        dataCache.saveRegisterWithEoriAndIdResponse(details)
      }

    for {
      response <- connector.register(RegisterWithEoriAndIdRequest(reqCommonGenerator.generate(), stripKFromUtr(value)))
      saved <- save(response, subscriptionDetails)
    } yield saved
  }

}
