
import sbt._
import sbt.Keys._
import sbtcompatibility.SbtCompatibilityPlugin.autoImport._
import sbtevictionrules.EvictionRulesPlugin.autoImport.evictionRules

import Aliases._

object Settings {

  private def scala212 = "2.12.11"
  private def scala213 = "2.13.5"

  private lazy val isAtLeastScala213 = Def.setting {
    import Ordering.Implicits._
    CrossVersion.partialVersion(scalaVersion.value).exists(_ >= (2, 13))
  }

  lazy val shared = Seq(
    scalaVersion := scala212,
    crossScalaVersions := Seq(scala212, scala213),
    scalacOptions ++= Seq(
      "-target:jvm-1.8",
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
          compilerPlugin(Deps.macroParadise) :: Nil
      }
    },
    scalacOptions ++= {
      if (isAtLeastScala213.value) Seq("-Ymacro-annotations")
      else Nil
    },
    autoAPIMappings := true,
    compatibilityRules ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "semver",
      "org.scala-js" %% "scalajs-library" % "semver",
      "org.typelevel" % "cats*" % "semver"
    ),
    compatibilityIgnored += "org.typelevel" %% "cats-macros",
    compatibilityIgnored += "org.typelevel" %% "cats-macros_sjs1",
    evictionRules ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "semver",
      "org.scala-js" %% "scalajs-library" % "semver",
      "org.typelevel" % "cats*" % "semver"
    )
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

}
