
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
  .nativeSettings(
    // See https://github.com/lihaoyi/utest/issues/144
    nativeLinkStubs := true
  )

lazy val testsJVM = tests.jvm
lazy val testsJS = tests.js
lazy val testsNative = tests.native

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

lazy val site = project
  .underDoc
  .enablePlugins(MicrositesPlugin)
  .settings(
    shared,
    dontPublish,
    micrositeName := "case-app",
    micrositeDescription := "Type-level argument parsing",
    micrositeBaseUrl := "/case-app",
    micrositeDocumentationUrl := "docs",
    micrositeAuthor := "Alexandre Archambault",
    micrositeHomepage := "https://alexarchambault.github.io/case-app",
    micrositeGithubOwner := "alexarchambault",
    micrositeGithubRepo := "case-app"
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
    readme,
    site
  )
  .settings(
    shared,
    dontPublish,
    name := "case-app-root",
    addMappingsToSiteDir(mappings.in(ScalaUnidoc, packageDoc), micrositeDocumentationUrl.in(site)),
    unidocProjectFilter.in(ScalaUnidoc, unidoc) := inAnyProject -- inProjects(
      utilJS,
      annotationsJS,
      coreJS,
      testsJS,
      readme,
      // for w/e reasons, these have to be excluded here, even though these are not included in the aggregation below
      utilNative,
      annotationsNative,
      coreNative,
      testsNative
    )
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
