import com.typesafe.sbt.packager.MappingsHelper._
import play.core.PlayVersion
import play.sbt.routes.RoutesKeys
import play.sbt.routes.RoutesKeys._
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.PublishingSettings._
import uk.gov.hmrc.gitstamp.GitStampPlugin.gitStampSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

import scala.language.postfixOps

mappings in Universal ++= directory(baseDirectory.value / "public")
// see https://stackoverflow.com/a/37180566

name := "customs-rosm-frontend"

targetJvm := "jvm-1.8"

scalaVersion := "2.12.14"

majorVersion := 2

PlayKeys.devSettings := Seq("play.server.http.port" -> "9830")

lazy val IntegrationTest = config("it") extend Test

val testConfig = Seq(IntegrationTest, Test)

lazy val microservice = (project in file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .configs(testConfig: _*)
  .settings(
    commonSettings,
    unitTestSettings,
    integrationTestSettings,
    playSettings,
    playPublishingSettings,
    scoverageSettings,
    twirlSettings,
    TwirlKeys.templateImports += "uk.gov.hmrc.customs.rosmfrontend.models._"
  )

def filterTestsOnPackageName(rootPackage: String): String => Boolean = { testName =>
  testName startsWith rootPackage
}

def forkedJvmPerTestConfig(tests: Seq[TestDefinition], packages: String*): Seq[Group] =
  tests.groupBy(_.name.takeWhile(_ != '.')).filter(packageAndTests => packages contains packageAndTests._1) map {
    case (packg, theTests) =>
      Group(packg, theTests, SubProcess(ForkOptions()))
  } toSeq

lazy val unitTestSettings =
  inConfig(Test)(Defaults.testTasks) ++
    Seq(
      testOptions in Test := Seq(Tests.Filter(filterTestsOnPackageName("unit"))),
      testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      fork in Test := true,
      unmanagedSourceDirectories in Test := Seq((baseDirectory in Test).value / "test"),
      addTestReportOption(Test, "test-reports")
    )

lazy val integrationTestSettings =
  inConfig(IntegrationTest)(Defaults.testTasks) ++
    Seq(
      testOptions in IntegrationTest := Seq(Tests.Filter(filterTestsOnPackageName("integration"))),
      testOptions in IntegrationTest += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
      fork in IntegrationTest := false,
      parallelExecution in IntegrationTest := false,
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := forkedJvmPerTestConfig((definedTests in Test).value, "integration")
    )

lazy val commonSettings: Seq[Setting[_]] = scalaSettings ++ publishingSettings ++ defaultSettings() ++ gitStampSettings

lazy val playSettings: Seq[Setting[_]] = Seq(
  routesImport ++= Seq("uk.gov.hmrc.customs.rosmfrontend.domain._"),
  RoutesKeys.routesImport += "uk.gov.hmrc.customs.rosmfrontend.models._"
)

lazy val twirlSettings: Seq[Setting[_]] = Seq(
  TwirlKeys.templateImports ++= Seq(
    "uk.gov.hmrc.customs.rosmfrontend.views.html._",
    "uk.gov.hmrc.customs.rosmfrontend.domain._"
  )
)

lazy val playPublishingSettings: Seq[sbt.Setting[_]] = Seq(credentials += SbtCredentials) ++
  publishAllArtefacts

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys

  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := List(
      "<empty>",
      "uk\\.gov\\.hmrc\\.customs\\.rosmfrontend\\.models\\.data\\..*",
      "uk\\.gov\\.hmrc\\.customs\\.rosmfrontend\\.view.*",
      "uk\\.gov\\.hmrc\\.customs\\.rosmfrontend\\.models.*",
      "uk\\.gov\\.hmrc\\.customs\\.rosmfrontend\\.config.*",
      ".*(Reverse|AuthService|BuildInfo|Routes|TestOnly).*"
    ).mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 88,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

val compileDependencies = Seq(
  "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "5.6.0",
  "uk.gov.hmrc" %% "http-caching-client" % "9.5.0-play-28",
  "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.9.0-play-28",
  "uk.gov.hmrc" %% "domain" % "6.0.0-play-28",
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % "0.71.0",
  "uk.gov.hmrc" %% "emailaddress" % "3.5.0",
  "uk.gov.hmrc" %% "logback-json-logger" % "5.1.0",
  "com.typesafe.play" %% "play-json-joda" % "2.8.1",
  "com.github.nscala-time" %% "nscala-time" % "2.32.0",
  "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-28" % "0.71.0"   % "test, it",
  "uk.gov.hmrc" %% "play-ui" % "9.6.0-play-28"
)

val testDependencies = Seq(
  "org.scalatest"          %% "scalatest"           % "3.2.12"            % "test,it",
  "com.typesafe.play"      %% "play-test"           % PlayVersion.current % "test,it",
  "org.scalatestplus.play" %% "scalatestplus-play"  % "5.1.0"             % "test,it",
  "org.mockito"             % "mockito-core"        % "4.7.0"             % "test,it",
  "org.scalatestplus"      %% "mockito-4-6"         % "3.2.13.0"          % "test, it",
  "org.scalacheck"         %% "scalacheck"          % "1.16.0"            % "test,it",
  "org.scalatestplus"      %% "scalacheck-1-15"     % "3.2.11.0"          % "test,it",
  "com.vladsch.flexmark"    % "flexmark-all"        % "0.62.0"   % "test,it",
  "com.github.tomakehurst" % "wiremock-standalone" % "2.27.2" % "test, it"
    exclude ("org.apache.httpcomponents", "httpclient") exclude ("org.apache.httpcomponents", "httpcore"),
  "org.jsoup" % "jsoup" % "1.15.3" % "test,it",
  "us.codecraft" % "xsoup" % "0.3.5" % "test,it",
  "uk.gov.hmrc" %% "webdriver-factory" % "0.38.0",
  "uk.gov.hmrc" %% "play-language" % "5.1.0-play-28",
  "org.pegdown" % "pegdown" % "1.6.0" % "test,it"
)

libraryDependencies ++= compileDependencies ++ testDependencies
