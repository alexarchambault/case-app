
import sbtcrossproject.CrossPlugin.autoImport.crossProject

import Aliases._
import Settings._

inThisBuild(List(
  organization := "com.github.alexarchambault",
  homepage := Some(url("https://github.com/alexarchambault/case-app")),
  licenses := Seq("Apache 2.0" -> url("http://opensource.org/licenses/Apache-2.0")),
  developers := List(
    Developer(
      "alexarchambault",
      "Alexandre Archambault",
      "",
      url("https://github.com/alexarchambault")
    )
  )
))

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
    name := "case-app",
    libraryDependencies += Deps.dataClass % Provided
  )

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val tests = crossProject(JSPlatform, JVMPlatform)
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

lazy val refined = crossProject(JSPlatform, JVMPlatform)
  .dependsOn(core)
  .settings(
    shared,
    caseAppPrefix,
    libs ++= Seq(
      Deps.refined.value,
      Deps.utest.value % "test"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val refinedJVM = refined.jvm
lazy val refinedJS = refined.js

lazy val readme = project
  .underDoc
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
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate(
    utilJVM,
    utilJS,
    annotationsJVM,
    annotationsJS,
    coreJVM,
    coreJS,
    testsJVM,
    testsJS,
    refinedJVM,
    refinedJS,
    readme
  )
  .settings(
    shared,
    dontPublish,
    name := "case-app-root",
    unidocProjectFilter.in(ScalaUnidoc, unidoc) := inAnyProject -- inProjects(
      utilJS,
      annotationsJS,
      coreJS,
      testsJS,
      refinedJS,
      readme
    )
  )

aliases(
  "validate" -> commandSeq("test", "tut")
)
