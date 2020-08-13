
import sbtcrossproject.crossProject

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
    caseAppPrefix,
    Mima.settings
  )

lazy val annotationsJVM = annotations.jvm
lazy val annotationsJS = annotations.js

lazy val util = crossProject(JSPlatform, JVMPlatform)
  .settings(
    shared,
    caseAppPrefix,
    Mima.settings,
    libs ++= Seq(
      Deps.shapeless.value,
      Deps.scalaCompiler.value % "provided",
      Deps.scalaReflect.value % "provided"
    )
  )

lazy val utilJVM = util.jvm
lazy val utilJS = util.js

lazy val cats = crossProject(JSPlatform, JVMPlatform)
  .dependsOn(core)
  .settings(
    shared,
    name := "case-app-cats",
    Mima.settings,
    mimaPreviousArtifacts := {
      mimaPreviousArtifacts.value.filter(_.revision != "2.0.0")
    },
    libs ++= Seq(
      Deps.catsEffect.value
    )
  )

lazy val catsJVM = cats.jvm
lazy val catsJS = cats.js

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .dependsOn(annotations, util)
  .settings(
    shared,
    name := "case-app",
    Mima.settings,
    libraryDependencies += Deps.dataClass % Provided
  )

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val tests = crossProject(JSPlatform, JVMPlatform)
  .disablePlugins(MimaPlugin)
  .dependsOn(cats % "test", core)
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
    Mima.settings,
    libs ++= Seq(
      Deps.refined.value,
      Deps.utest.value % "test"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val refinedJVM = refined.jvm
lazy val refinedJS = refined.js

disablePlugins(MimaPlugin)
skip.in(publish) := true
crossScalaVersions := Nil
