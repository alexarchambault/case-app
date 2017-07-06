
import sbt._
import sbt.Keys._

import sbtcrossproject.CrossPlugin.autoImport._

object Deps {

  import Def.setting

  def macroCompat = "org.typelevel" %% "macro-compat" % "1.1.1"
  def macroParadise = "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.patch
  def scalaCompiler = setting("org.scala-lang" % "scala-compiler" % scalaVersion.value)
  def scalaReflect = setting("org.scala-lang" % "scala-reflect" % scalaVersion.value)
  def scalaTest = setting("org.scalatest" %%% "scalatest" % "3.0.1")
  def shapeless = setting("com.chuusai" %%% "shapeless" % "2.3.2")

}