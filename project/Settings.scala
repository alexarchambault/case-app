
import sbt._
import sbt.Keys._

import Aliases._

import sbtcrossproject.CrossPlugin.autoImport._

object Settings {

  lazy val shared = Seq(
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation"
    ),
    scalacOptions ++= {
      scalaBinaryVersion.value match {
        case "2.10" | "2.11" =>
          Seq("-target:jvm-1.6")
        case _ =>
          Nil
      }
    },
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
    autoAPIMappings := true
  )

  lazy val dontPublish = Seq(
    publish := (),
    publishLocal := (),
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

}
