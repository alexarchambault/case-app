
import sbt._
import sbt.Keys._

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Deps {

  import Def.setting

  def dataClass = "io.github.alexarchambault" %% "data-class" % "0.2.2"
  def macroParadise = "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.patch
  def refined = setting("eu.timepit" %%% "refined" % "0.9.13")
  def scalaCompiler = setting("org.scala-lang" % "scala-compiler" % scalaVersion.value)
  def scalaReflect = setting("org.scala-lang" % "scala-reflect" % scalaVersion.value)
  def shapeless = setting("com.chuusai" %%% "shapeless" % "2.3.3")
  def utest = setting("com.lihaoyi" %%% "utest" % "0.7.4")

}
