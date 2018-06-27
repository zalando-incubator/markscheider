organization := "org.zalando"

name := "markscheider"

lazy val root = (project in file(".")).enablePlugins(PlayScala, TutPlugin)

scalaVersion := "2.12.2"
crossScalaVersions := Seq("2.11.11", "2.12.2")

val dropWizardMetricsVersion = "4.0.0"

libraryDependencies ++= Seq(
  "io.dropwizard.metrics" % "metrics-core" % dropWizardMetricsVersion,
  "io.dropwizard.metrics" % "metrics-json" % dropWizardMetricsVersion,
  "io.dropwizard.metrics" % "metrics-jvm" % dropWizardMetricsVersion,
  "io.dropwizard.metrics" % "metrics-logback" % dropWizardMetricsVersion,
  "com.google.code.findbugs" % "jsr305" % "3.0.2",
  "com.google.guava" % "guava" % "25.1-jre"
)

libraryDependencies += guice

//Test dependencies
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5",
  "org.scalacheck" %% "scalacheck" % "1.14.0"
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

//settings to compile readme
tutSourceDirectory := baseDirectory.value / "tut"
tutTargetDirectory := baseDirectory.value
