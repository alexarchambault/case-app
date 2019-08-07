
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

lazy val annotations = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(
    shared,
    caseAppPrefix
  )
  .nativeSettings(scalaVersion := Settings.scala211)

lazy val annotationsJVM = annotations.jvm
lazy val annotationsJS = annotations.js
lazy val annotationsNative = annotations.native

lazy val util = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(
    shared,
    caseAppPrefix,
    libs ++= Seq(
      Deps.shapeless.value,
      Deps.scalaCompiler.value % "provided",
      Deps.scalaReflect.value % "provided"
    ),
    unmanagedSourceDirectories.in(Compile) ++= {
      val current = unmanagedSourceDirectories.in(Compile).value
      val is211Plus = CrossVersion.partialVersion(scalaVersion.value).exists {
        case (major, minor) => major == 2 && minor >= 11
      }
      if (is211Plus)
        current.collect {
	  case dir if dir.getName == "scala" =>
	    dir.getParentFile / "scala-2.11+"
        }
      else
        Nil
    }
  )
  .nativeSettings(scalaVersion := Settings.scala211)

lazy val utilJVM = util.jvm
lazy val utilJS = util.js
lazy val utilNative = util.native

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .dependsOn(annotations, util)
  .settings(
    shared,
    name := "case-app"
  )
  .nativeSettings(scalaVersion := Settings.scala211)

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
  .nativeSettings(
    scalaVersion := Settings.scala211,
    // See https://github.com/lihaoyi/utest/issues/144
    nativeLinkStubs := true
  )

lazy val testsJVM = tests.jvm
lazy val testsJS = tests.js
lazy val testsNative = tests.native

lazy val refined = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .dependsOn(core)
  .settings(
    shared,
    caseAppPrefix,
    onlyIn("2.11", "2.12"), // refined not published for 2.13.0-RC1 for now
    libs ++= Seq(
      Deps.refined.value,
      Deps.utest.value % "test"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  .nativeSettings(
    scalaVersion := Settings.scala211,
    // See https://github.com/lihaoyi/utest/issues/144
    nativeLinkStubs := true
  )

lazy val refinedJVM = refined.jvm
lazy val refinedJS = refined.js
lazy val refinedNative = refined.native

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
      readme,
      // for w/e reasons, these have to be excluded here, even though these are not included in the aggregation below
      utilNative,
      annotationsNative,
      coreNative,
      testsNative,
      refinedNative
    )
  )

lazy val native = project
  .in(file("target/native")) // dummy dir
  .aggregate(
    utilNative,
    annotationsNative,
    coreNative,
    testsNative,
    refinedNative
  )
  .settings(
    shared,
    dontPublish
  )

aliases(
  "validate" -> commandSeq("test", "tut")
)
