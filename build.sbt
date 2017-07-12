
import sbtcrossproject.CrossPlugin.autoImport.crossProject

import Aliases._
import Settings._

lazy val annotations = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(
    shared,
    caseAppPrefix
  )

lazy val annotationsJVM = annotations.jvm
lazy val annotationsJS = annotations.js
lazy val annotationsNative = annotations.native

lazy val util = crossProject(JSPlatform, JVMPlatform, NativePlatform)
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
lazy val utilNative = util.native

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .dependsOn(annotations, util)
  .settings(
    shared,
    name := "case-app"
  )

lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val coreNative = core.native

lazy val tests = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .dependsOn(core)
  .settings(
    shared,
    caseAppPrefix,
    dontPublish,
    libs += Deps.utest.value % "test",
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val testsJVM = tests.jvm
lazy val testsJS = tests.js
lazy val testsNative = tests.native

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

lazy val native = project
  .in(file("target/native")) // dummy dir
  .aggregate(
    utilNative,
    annotationsNative,
    coreNative,
    testsNative
  )
  .settings(
    shared,
    dontPublish
  )

aliases(
  "validate" -> commandSeq("test", "tut")
)
