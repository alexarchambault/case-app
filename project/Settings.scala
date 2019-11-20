
import sbt._
import sbt.Def.setting
import sbt.Keys._

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

import Aliases._

object Settings {

  def scala211 = Deps.scalaVersions.find(_.startsWith("2.11.1")).get

  lazy val shared = Seq(
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation"
    ),
    libs ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v >= 13 =>
          // if scala 2.13.0-M4 or later, macro annotations merged into scala-reflect
          // https://github.com/scala/scala/pull/6606
          Nil
        case _ =>
          compilerPlugin(Deps.paradise) :: Nil
      }
    },
    autoAPIMappings := true
  )

  lazy val dontPublish = Seq(
    publish := (()),
    publishLocal := (()),
    publishArtifact := false
  )

  lazy val caseAppPrefix = {
    name := {
      val shortenedName = name.value
        .stripSuffix("JVM")
        .stripSuffix("JS")
        .stripSuffix("Native")
      "case-app-" + shortenedName
    }
  }

  def onlyIn(sbv: String*) = {

    val sbv0 = sbv.toSet
    val ok = Def.setting {
      CrossVersion.partialVersion(scalaBinaryVersion.value)
        .map { case (maj, min) => s"$maj.$min" }
        .exists(sbv0)
    }

    Seq(
      baseDirectory := {
        val baseDir = baseDirectory.value

        if (ok.value)
          baseDir
        else
          baseDir / "target" / "dummy"
      },
      libraryDependencies := {
        val deps = libraryDependencies.value
        if (ok.value)
          deps
        else
          Nil
      },
      publishArtifact := ok.value
    )
  }

  def scalaCompiler = setting("org.scala-lang" % "scala-compiler" % scalaVersion.value)
  def scalaReflect = setting("org.scala-lang" % "scala-reflect" % scalaVersion.value)
  def utest = setting {
    val sv = scalaVersion.value
    val ver =
      if (sv.startsWith("2.11.")) "0.6.7"
      else Deps.V.utest

    "com.lihaoyi" %%% "utest" % ver
  }


}
