name := "case-app"

organization := "com.github.alexarchambault"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.scala-lang"  % "scala-reflect" % scalaVersion.value
,     "org.scalaz" %% "scalaz-core"   % "7.1.0"
,    "com.chuusai" %% "shapeless"     % "2.0.0" cross CrossVersion.full
,  "org.scalatest" %% "scalatest"     % "2.2.0" % "test"
)

scalacOptions ++= Seq(
  "-feature"
, "-deprecation"
)
