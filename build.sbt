organization := "org.zalando"

name := "markscheider"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "io.dropwizard.metrics" % "metrics-core" % "3.1.0",
  "io.dropwizard.metrics" % "metrics-json" % "3.1.0",
  "io.dropwizard.metrics" % "metrics-jvm" % "3.1.0",
  "io.dropwizard.metrics" % "metrics-logback" % "3.1.0",

  "com.google.code.findbugs" % "jsr305" % "2.0.0",
  "com.google.guava" % "guava" % "18.0"
)

//Test dependencies
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.6",
  "org.scalacheck" %% "scalacheck" % "1.12.5"
) map (_ % "test")

maintainer := "team-kohle@zalando.de"
licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))

//pom extra info
publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomExtra := (
  <scm>
    <url>git@github.com:zalando-incubator/markscheider.git</url>
    <developerConnection>scm:git:git@github.com:zalando-incubator/markscheider.git</developerConnection>
    <connection>scm:git:https://github.com/zalando-incubator/markscheider.git</connection>
  </scm>
    <developers>
      <developer>
        <name>Lena Brueder</name>
        <email>lena.brueder@zalando.de</email>
        <url>https://github.com/zalando</url>
      </developer>
    </developers>)

homepage := Some(url("https://github.com/zalando-incubator/markscheider"))

//settings to compile readme
tutSettings
tutSourceDirectory := baseDirectory.value / "tut"
tutTargetDirectory := baseDirectory.value
