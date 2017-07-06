
import sbtcrossproject.CrossPlugin.autoImport.crossProject

import Aliases._
import Settings._

lazy val annotations = crossProject(JSPlatform, JVMPlatform)
  .settings(
    shared,
    caseAppPrefix
  )

lazy val annotationsJVM = annotations.jvm
lazy val annotationsJS = annotations.js

lazy val util = crossProject(JSPlatform, JVMPlatform)
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

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .dependsOn(annotations, util)
  .settings(
    shared,
    name := "case-app"
  )

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val tests = crossProject(JSPlatform, JVMPlatform)
  .dependsOn(core)
  .settings(
    shared,
    caseAppPrefix,
    dontPublish,
    libs += Deps.scalaTest.value % "test"
  )

lazy val testsJVM = tests.jvm
lazy val testsJS = tests.js

lazy val doc = project
  .dependsOn(coreJVM)
  .enablePlugins(TutPlugin)
  .settings(
    shared,
    dontPublish,
    tutSourceDirectory := baseDirectory.value,
    tutTargetDirectory := baseDirectory.in(LocalRootProject).value
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
    testsJVM,
    testsJS,
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
