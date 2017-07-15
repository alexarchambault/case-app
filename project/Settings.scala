
import sbt._
import sbt.Keys._

import Aliases._

import sbtcrossproject.CrossPlugin.AutoImport.crossPlatform
import scala.scalanative.sbtplugin.NativePlatform

object Settings {

  lazy val shared = Seq(
    organization := "com.github.alexarchambault",
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
    libs += compilerPlugin(Deps.macroParadise),
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    licenses := Seq("Apache 2.0" -> url("http://opensource.org/licenses/Apache-2.0")),
    homepage := Some(url("https://github.com/alexarchambault/case-app")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/alexarchambault/case-app.git"),
      "scm:git:github.com/alexarchambault/case-app.git",
      Some("scm:git:git@github.com:alexarchambault/case-app.git")
    )),
    developers := List(Developer(
      "alexarchambault",
      "Alexandre Archambault",
      "",
      url("https://github.com/alexarchambault")
    )),
    credentials ++= {
      Seq("SONATYPE_USER", "SONATYPE_PASS").map(sys.env.get) match {
        case Seq(Some(user), Some(pass)) =>
          Seq(Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass))
        case _ =>
          Seq.empty
      }
    }
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
