
lazy val `case-app` = project.in(file("."))
  .aggregate(utilJVM, utilJS, annotationsJVM, annotationsJS, coreJVM, coreJS, doc)
  .dependsOn(utilJVM, annotationsJVM, coreJVM)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    name := "case-app-root"
  )

lazy val annotations = crossProject
  .settings(commonSettings: _*)
  .settings(
    name := "case-app-annotations"
  )

lazy val annotationsJVM = annotations.jvm
lazy val annotationsJS = annotations.js

lazy val util = crossProject
  .settings(commonSettings: _*)
  .settings(
    name := "case-app-util",
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.1",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      "org.typelevel" %% "macro-compat" % "1.1.1",
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
    )
  )

lazy val utilJVM = util.jvm
lazy val utilJS = util.js

lazy val core = crossProject
  .dependsOn(annotations, util)
  .settings(commonSettings: _*)
  .settings(
    name := "case-app",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.0.0-M11" % "test"
    )
  )

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val doc = project
  .dependsOn(coreJVM)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(tutSettings)
  .settings(
    tutSourceDirectory := baseDirectory.value,
    tutTargetDirectory := baseDirectory.value / ".."
  )


lazy val commonSettings = Seq(
  organization := "com.github.alexarchambault",
  scalaVersion := "2.11.8",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases")
  ),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-target:jvm-1.6"
  ),
  libraryDependencies ++= {
    if (scalaVersion.value startsWith "2.10.")
      Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
    else
      Seq()
  }
) ++ fullReleaseSettings

lazy val fullReleaseSettings = Seq(
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
  credentials ++= {
    Seq("SONATYPE_USER", "SONATYPE_PASS").map(sys.env.get) match {
      case Seq(Some(user), Some(pass)) =>
        Seq(Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass))
      case _ =>
        Seq.empty
    }
  }
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

addCommandAlias("validate", Seq(
  "test",
  "tut"
).mkString(";", ";", ""))
