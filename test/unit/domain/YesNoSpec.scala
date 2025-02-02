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

package unit.domain

import uk.gov.hmrc.customs.rosmfrontend.domain.YesNo
import util.UnitSpec

class YesNoSpec extends UnitSpec {
  "YesNo" should {
    "when true, have isNo false" in {
      YesNo(true).isNo shouldBe false
    }
    "when false, have isNo true" in {
      YesNo(false).isNo shouldBe true
    }
  }
}
