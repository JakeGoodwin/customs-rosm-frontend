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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.{JsValue, Json}
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.MimeTypes.JSON
import play.mvc.Http.Status._

object EnrolmentStoreProxyService {

  private val responseWithOk: JsValue =
    Json.parse {
      """{
        |	"startRecord": 1,
        |	"totalRecords": 2,
        |	"enrolments": [{
        |			"service": "HMRC-CUS-ORG",
        |			"state": "NotYetActivated",
        |			"friendlyName": "My First Client's SA Enrolment",
        |			"enrolmentDate": "2018-10-05T14:48:00.000Z",
        |			"failedActivationCount": 1,
        |			"activationDate": "2018-10-13T17:36:00.000Z",
        |			"identifiers": [{
        |				"key": "UTR",
        |				"value": "1111111111"
        |			}]
        |		},
        |		{
        |			"service": "HMRC-CUS-ORG",
        |			"state": "Activated",
        |			"friendlyName": "My Second Client's SA Enrolment",
        |			"enrolmentDate": "2017-06-25T12:24:00.000Z",
        |			"failedActivationCount": 1,
        |			"activationDate": "2017-07-01T09:52:00.000Z",
        |			"identifiers": [{
        |				"key": "UTR",
        |				"value": "2234567890"
        |			}]
        |		}
        |	]
        |}""".stripMargin
    }

  private val responseWitNoContent: JsValue =
    Json.parse {
      """{}""".stripMargin
    }

  private val responseWithGroups = Json.parse { """{
                                                  |    "principalGroupIds": [
                                                  |       "ABCEDEFGI1234567",
                                                  |       "ABCEDEFGI1234568"
                                                  |    ],
                                                  |    "delegatedGroupIds": [
                                                  |       "ABCEDEFGI1234567",
                                                  |       "ABCEDEFGI1234568"
                                                  |    ]
                                                  |}""".stripMargin }

  private def endpointForCds(groupId: String) =
    s"/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments?type=principal&service=HMRC-CUS-ORG"

  private def endpointForAll(groupId: String) =
    s"/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments?type=principal"

  private def endpointForGroups(enrolmentKey: String) =
    s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups"

  val endpointPatternForGroups = "/enrolment-store-proxy/enrolment-store/enrolments/HMRC-CUS-ORG.*/groups"

  def returnGroupsForThisEnrolmentKey(enrolmentKey: String): Unit =
    stubTheEnrolmentStoreProxyResponse(endpointForGroups(enrolmentKey), responseWithGroups.toString(), OK)

  def returnGroupsForThisEnrolmentKeyNoContent(enrolmentKey: String): Unit =
    stubTheEnrolmentStoreProxyResponse(endpointForGroups(enrolmentKey), responseWitNoContent.toString(), NO_CONTENT)

  def returnGroupsForThisEnrolmentKeyPatternMatchNoContent(): Unit =
    stubTheEnrolmentStoreUrlMatchProxyResponse(endpointPatternForGroups, responseWitNoContent.toString(), NO_CONTENT)

  def returnEnrolmentStoreProxyResponseOkForCds(groupId: String): Unit =
    stubTheEnrolmentStoreProxyResponse(endpointForCds(groupId), responseWithOk.toString(), OK)

  def returnEnrolmentStoreProxyResponseNoContentForCds(groupId: String): Unit =
    stubTheEnrolmentStoreProxyResponse(endpointForCds(groupId), responseWitNoContent.toString(), NO_CONTENT)

  def returnEnrolmentStoreProxyResponseOkForAll(groupId: String): Unit =
    stubTheEnrolmentStoreProxyResponse(endpointForAll(groupId), responseWithOk.toString(), OK)

  def returnEnrolmentStoreProxyResponseNoContentForAll(groupId: String): Unit =
    stubTheEnrolmentStoreProxyResponse(endpointForAll(groupId), responseWitNoContent.toString(), NO_CONTENT)

  def stubTheEnrolmentStoreProxyResponse(url: String, response: String, status: Int): Unit =
    stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

  def stubTheEnrolmentStoreUrlMatchProxyResponse(url: String, response: String, status: Int): Unit =
    stubFor(
      get(urlMatching(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )
}
