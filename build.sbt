import sbt.Keys._

lazy val GatlingTest = config("gatling") extend Test

scalaVersion := "2.11.11"

libraryDependencies += jdbc
libraryDependencies += evolutions
libraryDependencies += "com.typesafe.play" %% "anorm" % "2.5.0"
libraryDependencies += "com.netaporter" %% "scala-uri" % "0.4.14"
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.1.0"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.2" % Test
libraryDependencies += "io.gatling" % "gatling-test-framework" % "2.2.2" % Test
    
// The Play project itself
lazy val root = (project in file("."))
  .enablePlugins(Common, PlayScala, GatlingPlugin)
  .configs(GatlingTest)
  .settings(inConfig(GatlingTest)(Defaults.testSettings): _*)
  .settings(
    name := """slack-bob""",
    scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"
  )

