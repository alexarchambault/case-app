import sbtcrossproject.crossProject

import Settings._

inThisBuild(List(
  organization := "com.github.alexarchambault",
  homepage     := Some(url("https://github.com/alexarchambault/case-app")),
  licenses     := Seq("Apache 2.0" -> url("http://opensource.org/licenses/Apache-2.0")),
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
  .nativeConfigure(_.disablePlugins(MimaPlugin))
  .jvmSettings(Mima.settings)
  .jsSettings(Mima.settings)
  .settings(
    shared,
    caseAppPrefix
  )

lazy val annotationsJVM    = annotations.jvm
lazy val annotationsJS     = annotations.js
lazy val annotationsNative = annotations.native

lazy val util = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .nativeConfigure(_.disablePlugins(MimaPlugin))
  .jvmSettings(Mima.settings)
  .jsSettings(Mima.settings)
  .settings(
    shared,
    caseAppPrefix,
    libraryDependencies ++= {
      val sv = scalaVersion.value
      if (sv.startsWith("2."))
        Seq(
          Deps.shapeless.value,
          Deps.scalaCompiler.value % "provided",
          Deps.scalaReflect.value  % "provided"
        )
      else
        Nil
    }
  )

lazy val utilJVM    = util.jvm
lazy val utilJS     = util.js
lazy val utilNative = util.native

lazy val cats = crossProject(JSPlatform, JVMPlatform)
  .dependsOn(core)
  .jvmSettings(Mima.settings)
  .jsSettings(Mima.settings)
  .settings(
    shared,
    name := "case-app-cats",
    libraryDependencies ++= Seq(
      Deps.catsEffect3.value
    ),
    libraryDependencies += Deps.utest.value % Test,
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val catsJVM = cats.jvm
lazy val catsJS  = cats.js

lazy val cats2 = crossProject(JSPlatform, JVMPlatform)
  .dependsOn(core)
  .jvmSettings(Mima.settings)
  .jsSettings(Mima.settings)
  .settings(
    shared,
    name              := "case-app-cats-effect-2",
    Compile / sources := (catsJVM / Compile / sources).value,
    libraryDependencies ++= Seq(
      Deps.catsEffect2.value
    ),
    libraryDependencies += Deps.utest.value % Test,
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val cats2JVM = cats2.jvm
lazy val cats2JS  = cats2.js

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .nativeConfigure(_.disablePlugins(MimaPlugin))
  .dependsOn(annotations, util)
  .jvmSettings(Mima.settings)
  .jsSettings(Mima.settings)
  .settings(
    shared,
    name := "case-app",
    libraryDependencies ++= {
      val sv = scalaVersion.value
      val maybeDataClass =
        if (sv.startsWith("2.")) Seq(Deps.dataClass % Provided)
        else Nil
      Seq(Deps.sourcecode.value) ++ maybeDataClass
    }
  )

lazy val coreJVM    = core.jvm
lazy val coreJS     = core.js
lazy val coreNative = core.native

lazy val tests = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .disablePlugins(MimaPlugin)
  .dependsOn(core)
  .settings(
    shared,
    caseAppPrefix,
    publish / skip := true,
    libraryDependencies ++= Seq(
      Deps.pprint.value % Test,
      Deps.utest.value  % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val testsJVM    = tests.jvm
lazy val testsJS     = tests.js
lazy val testsNative = tests.native

disablePlugins(MimaPlugin)
publish / skip     := true
crossScalaVersions := Nil

Global / onChangedBuildSource := ReloadOnSourceChanges
