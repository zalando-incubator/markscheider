organization := "org.zalando"

name := "markscheider"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"
crossScalaVersions := Seq("2.11.12", "2.12.8")

val dropWizardMetricsVersion = "4.0.5"

libraryDependencies ++= Seq(
  "io.dropwizard.metrics" % "metrics-core" % dropWizardMetricsVersion,
  "io.dropwizard.metrics" % "metrics-json" % dropWizardMetricsVersion,
  "io.dropwizard.metrics" % "metrics-jvm" % dropWizardMetricsVersion,
  "io.dropwizard.metrics" % "metrics-logback" % dropWizardMetricsVersion
)

libraryDependencies += guice

//Test dependencies
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5",
  "org.scalacheck" %% "scalacheck" % "1.14.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.0"
) map (_ % "test")

maintainer := "Matthias Erche <matthias.erche@zalando.de>"
licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))

//pom extra info
publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomExtra := (<scm>
    <url>git@github.com:zalando-incubator/markscheider.git</url>
    <developerConnection>scm:git:git@github.com:zalando-incubator/markscheider.git</developerConnection>
    <connection>scm:git:https://github.com/zalando-incubator/markscheider.git</connection>
  </scm>
    <developers>
      <developer>
        <name>Matthias Erche</name>
        <email>matthias.erche@zalando.de</email>
        <url>https://github.com/zalando-incubator</url>
      </developer>
    </developers>)

homepage := Some(url("https://github.com/zalando-incubator/markscheider"))
