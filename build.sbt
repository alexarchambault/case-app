
import Aliases._
import Settings._

lazy val annotations = crossProject
  .settings(
    shared,
    caseAppPrefix
  )

lazy val annotationsJVM = annotations.jvm
lazy val annotationsJS = annotations.js

lazy val util = crossProject
  .settings(
    shared,
    caseAppPrefix,
    libs ++= Seq(
      Deps.shapeless.value,
      Deps.macroCompat,
      Deps.scalaCompiler.value % "provided",
      Deps.scalaReflect.value % "provided"
    )
  )

lazy val utilJVM = util.jvm
lazy val utilJS = util.js

lazy val core = crossProject
  .dependsOn(annotations, util)
  .settings(
    shared,
    name := "case-app",
    libs += Deps.scalaTest.value % "test"
  )

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val doc = project
  .dependsOn(coreJVM)
  .settings(
    shared,
    dontPublish,
    tutSettings,
    tutSourceDirectory := baseDirectory.value,
    tutTargetDirectory := baseDirectory.value / ".."
  )

lazy val `case-app` = project
  .in(root)
  .aggregate(
    utilJVM,
    utilJS,
    annotationsJVM,
    annotationsJS,
    coreJVM,
    coreJS,
    doc
  )
  .settings(
    shared,
    dontPublish,
    name := "case-app-root"
  )

aliases(
  "validate" -> commandSeq("test", "tut")
)