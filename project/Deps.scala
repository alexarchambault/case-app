
import sbt._
import sbt.Keys._

import sbtcrossproject.CrossPlugin.autoImport._
import scala.scalanative.sbtplugin.NativePlatform

object Deps {

  import Def.setting

  def macroCompat = "org.typelevel" %% "macro-compat" % "1.1.1"
  def macroParadise = "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.patch
  def scalaCompiler = setting("org.scala-lang" % "scala-compiler" % scalaVersion.value)
  def scalaReflect = setting("org.scala-lang" % "scala-reflect" % scalaVersion.value)
  def shapeless = setting {
    if (crossPlatform.value == NativePlatform)
      "com.github.alexarchambault" %%% "shapeless" % "2.3.3-pre-1"
    else
      "com.chuusai" %%% "shapeless" % "2.3.2"
  }
  def utest = setting("com.lihaoyi" %%% "utest" % "0.5.4")

}
