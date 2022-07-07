import sbt._
import sbt.Keys._

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Deps {

  import Def.setting

  def catsEffect2   = setting("org.typelevel" %%% "cats-effect" % "2.5.5")
  def catsEffect3   = setting("org.typelevel" %%% "cats-effect" % "3.3.12")
  def dataClass     = "io.github.alexarchambault" %% "data-class" % "0.2.5"
  def macroParadise = "org.scalamacros"            % "paradise"   % "2.1.1" cross CrossVersion.patch
  def refined       = setting("eu.timepit" %%% "refined" % "0.10.1")
  def scalaCompiler = setting("org.scala-lang" % "scala-compiler" % scalaVersion.value)
  def scalaReflect  = setting("org.scala-lang" % "scala-reflect" % scalaVersion.value)
  def shapeless     = setting("com.chuusai" %%% "shapeless" % "2.3.9")
  def sourcecode    = setting("com.lihaoyi" %%% "sourcecode" % "0.2.8")
  def utest         = setting("com.lihaoyi" %%% "utest" % "0.7.11")

}
