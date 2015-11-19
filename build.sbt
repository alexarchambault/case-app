
lazy val `case-app` = crossProject.in(file("."))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "case-app",
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.0-SNAPSHOT",
      "com.github.alexarchambault" %%% "derive" % "0.1.0-SNAPSHOT",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "org.scalatest" %%% "scalatest" % "3.0.0-M11" % "test"
    ),
    libraryDependencies ++= {
      if (scalaVersion.value startsWith "2.10.")
        Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
      else
        Seq()
    }
  )

lazy val `case-app-jvm` = `case-app`.jvm
lazy val `case-app-js` = `case-app`.js

lazy val doc = project
  .dependsOn(`case-app-jvm`)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(tutSettings)
  .settings(
    tutSourceDirectory := baseDirectory.value,
    tutTargetDirectory := baseDirectory.value / ".."
  )


lazy val commonSettings = Seq(
  organization := "com.github.alexarchambault",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.6", "2.11.7"),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-target:jvm-1.7"
  )
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  licenses := Seq("Apache 2.0" -> url("http://opensource.org/licenses/Apache-2.0")),
  homepage := Some(url("https://github.com/alexarchambault/case-app")),
  pomExtra := {
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
  },
  credentials += {
    Seq("SONATYPE_USER", "SONATYPE_PASS").map(sys.env.get) match {
      case Seq(Some(user), Some(pass)) =>
        Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
      case _ =>
        Credentials(Path.userHome / ".ivy2" / ".credentials")
    }
  },
  ReleaseKeys.versionBump := sbtrelease.Version.Bump.Bugfix,
  ReleaseKeys.publishArtifactsAction := PgpKeys.publishSigned.value
) ++ releaseSettings

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

