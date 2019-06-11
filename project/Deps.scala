
import sbt._
import sbt.Keys._

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Deps {

  import Def.setting

  def macroParadise = "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch
  def refined = setting("eu.timepit" %%% "refined" % "0.9.8")
  def scalaCompiler = setting("org.scala-lang" % "scala-compiler" % scalaVersion.value)
  def scalaReflect = setting("org.scala-lang" % "scala-reflect" % scalaVersion.value)
  def shapeless = setting("com.chuusai" %%% "shapeless" % "2.3.3")
  def utest = setting {
    val sv = scalaVersion.value
    val ver =
      if (sv.startsWith("2.11.")) "0.6.7"
      else "0.6.9"
    "com.lihaoyi" %%% "utest" % ver
  }

}
