import SonatypeKeys._

//import _root_.sbtbuildinfo.Plugin.BuildInfoKey

import com.typesafe.sbt.SbtGit.GitKeys._

name := "case-app"

version := "0.1.0"

organization := "com.github.alexarchambault"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "org.scala-lang"  % "scala-reflect" % scalaVersion.value
,     "org.scalaz" %% "scalaz-core"   % "7.1.0"
,    "com.chuusai" %% "shapeless"     % "2.0.0"
,  "org.scalatest" %% "scalatest"     % "2.2.0" % "test"
)

scalacOptions ++= Seq(
  "-feature"
, "-deprecation"
)


xerial.sbt.Sonatype.sonatypeSettings

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := {
  <url>https://github.com/alexarchambault/case-app</url>
    <licenses>
      <license>
        <name>Apache 2.0</name>
        <url>http://opensource.org/licenses/Apache-2.0</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com/alexarchambault/case-app.git</connection>
      <developerConnection>scm:git:git@github.com:alexarchambault/case-app.git</developerConnection>
      <url>github.com/alexarchambault/case-app.git</url>
    </scm>
    <developers>
      <developer>
        <id>alexarchambault</id>
        <name>Alexandre Archambault</name>
        <url>https://github.com/alexarchambault</url>
      </developer>
    </developers>
}

