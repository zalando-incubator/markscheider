import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

// we should consider using '-Xfatal-warnings' to be very strict
scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  //"-unchecked",
  //"-Xfatal-warnings",
  "-Xcheckinit",
  "-Xlint"
)

// this checks violations to the paypal style guide
scalastyleConfigUrl := Option(url("https://s3.eu-central-1.amazonaws.com/kohle-development/scalastyle-config.xml"))

scalastyleFailOnError := true

// Create a default Scala style task to run with tests
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value

(compileInputs in(Compile, compile)) <<= (compileInputs in(Compile, compile)) dependsOn compileScalastyle

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// configure scalariform to use preferred settings
scalariformSettings

//pls adjust your intellij settings accordingly
ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
  .setPreference(PreserveSpaceBeforeArguments, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(IndentLocalDefs, true)
  .setPreference(IndentPackageBlocks, true)
  .setPreference(IndentWithTabs, false)
  .setPreference(IndentSpaces, 2)
  .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
  .setPreference(PreserveDanglingCloseParenthesis, true)

coverageEnabled in Test:= true
coverageExcludedPackages := "<empty>;router.Routes;router.RoutesPrefix;controllers.Reverse*;controllers.javascript.Reverse*"
coverageMinimum := 50

//do not warn in test cases when we use reflective calls - but do so in production!
scalacOptions in Test += "-language:reflectiveCalls"

scapegoatVersion := "1.2.0"
scapegoatDisabledInspections := Seq(
  "ObjectNames",
  "ClassNames"
)
scapegoatIgnoredFiles := Seq(".*/routes/main/.*")

publish <<= publish dependsOn scapegoat
publishLocal <<= publishLocal dependsOn scapegoat
