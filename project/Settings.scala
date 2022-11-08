import sbt._
import sbt.Keys._

object Settings {

  private def scala212 = "2.12.17"
  private def scala213 = "2.13.10"
  private def scala3   = "3.2.0"

  private lazy val isAtLeastScala213 = Def.setting {
    import Ordering.Implicits._
    CrossVersion.partialVersion(scalaVersion.value).exists(_ >= (2, 13))
  }

  lazy val shared = Seq(
    scalaVersion       := scala3,
    crossScalaVersions := Seq(scala212, scala213, scala3),
    scalacOptions ++= Seq(
      "-target:jvm-1.8",
      "-feature",
      "-deprecation"
    ),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          compilerPlugin(Deps.macroParadise) :: Nil
        case _ =>
          // if scala 2.13.0-M4 or later, macro annotations merged into scala-reflect
          // https://github.com/scala/scala/pull/6606
          Nil
      }
    },
    scalacOptions ++= {
      if (isAtLeastScala213.value) Seq("-Ymacro-annotations")
      else Nil
    },
    autoAPIMappings := true
  )

  lazy val caseAppPrefix =
    name := {
      val shortenedName = name.value
        .stripSuffix("JVM")
        .stripSuffix("JS")
        .stripSuffix("Native")
      "case-app-" + shortenedName
    }

}
