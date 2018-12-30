
import sbt._
import sbt.Keys._

import Aliases._

object Settings {

  def scala211 = "2.11.12"
  private def scala212 = "2.12.7"
  private def scala213 = "2.13.0-M5"

  lazy val shared = Seq(
    scalaVersion := scala212,
    crossScalaVersions := Seq(scala212, scala211, scala213),
    resolvers += Resolver.sonatypeRepo("releases"),
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
          compilerPlugin(Deps.macroParadise) :: Nil
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

}
