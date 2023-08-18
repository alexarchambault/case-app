import $ivy.`com.github.lolgab::mill-mima::0.0.23`

import com.github.lolgab.mill.mima.Mima
import mill._
import mill.scalajslib._
import mill.scalalib._
import mill.scalanativelib._

import scala.concurrent.duration.DurationInt

object Versions {
  def scala212 = "2.12.18"
  def scala213 = "2.13.11"
  def scala3   = "3.3.0"
  def scala    = Seq(scala212, scala213, scala3)

  def scalaJs     = "1.13.2"
  def scalaNative = "0.4.14"
}

object Deps {
  def catsEffect                = ivy"org.typelevel::cats-effect::3.5.1"
  def catsEffect2               = ivy"org.typelevel::cats-effect::2.5.5"
  def dataClass                 = ivy"io.github.alexarchambault::data-class:0.2.6"
  def macroParadise             = ivy"org.scalamacros:::paradise:2.1.1"
  def pprint                    = ivy"com.lihaoyi::pprint::0.8.1"
  def scalaCompiler(sv: String) = ivy"org.scala-lang:scala-compiler:$sv"
  def scalaReflect(sv: String)  = ivy"org.scala-lang:scala-reflect:$sv"
  def shapeless                 = ivy"com.chuusai::shapeless::2.3.10"
  def sourcecode                = ivy"com.lihaoyi::sourcecode::0.3.0"
  def utest                     = ivy"com.lihaoyi::utest::0.8.1"
}

object annotations extends Module {
  object jvm    extends Cross[AnnotationsJvm](Versions.scala)
  object js     extends Cross[AnnotationsJs](Versions.scala)
  object native extends Cross[AnnotationsNative](Versions.scala)

  trait AnnotationsJvm extends CrossSbtModule with CrossSources with CaseAppPublishModule
      with MimaChecks
  trait AnnotationsJs extends CrossSbtModule with CaseAppScalaJsModule with CrossSources
      with CaseAppPublishModule with MimaChecks
  trait AnnotationsNative extends CrossSbtModule with CaseAppScalaNativeModule with CrossSources
      with CaseAppPublishModule
}

object util extends Module {
  object jvm    extends Cross[UtilJvm](Versions.scala)
  object js     extends Cross[UtilJs](Versions.scala)
  object native extends Cross[UtilNative](Versions.scala)

  trait UtilJvm    extends Util with MimaChecks
  trait UtilJs     extends Util with CaseAppScalaJsModule with MimaChecks
  trait UtilNative extends Util with CaseAppScalaNativeModule

  trait Util extends CrossSbtModule with CrossSources with CaseAppPublishModule {
    def ivyDeps = T {
      if (scalaVersion().startsWith("2."))
        Agg(Deps.shapeless)
      else
        Agg()
    }
    def compileIvyDeps = T {
      if (scalaVersion().startsWith("2."))
        Agg(
          Deps.scalaCompiler(scalaVersion()),
          Deps.scalaReflect(scalaVersion())
        )
      else
        Agg()
    }
  }
}

object core extends Module {
  object jvm    extends Cross[CoreJvm](Versions.scala)
  object js     extends Cross[CoreJs](Versions.scala)
  object native extends Cross[CoreNative](Versions.scala)

  trait CoreJvm extends Core with MimaChecks {
    def moduleDeps = Seq(
      annotations.jvm(),
      util.jvm()
    )
  }
  trait CoreJs extends Core with CaseAppScalaJsModule with MimaChecks {
    def moduleDeps = Seq(
      annotations.js(),
      util.js()
    )
  }
  trait CoreNative extends Core with CaseAppScalaNativeModule {
    def moduleDeps = Seq(
      annotations.native(),
      util.native()
    )
  }

  trait Core extends CrossSbtModule with CrossSources with CaseAppPublishModule {
    def ivyDeps = T {
      val maybeDataClass =
        if (scalaVersion().startsWith("2.")) Agg(Deps.dataClass)
        else Agg.empty
      Agg(Deps.sourcecode) ++ maybeDataClass
    }
    def scalacOptions = T {
      val maybeMacroAnn =
        if (scalaVersion().startsWith("2.13."))
          Seq("-Ymacro-annotations")
        else
          Nil
      super.scalacOptions() ++ maybeMacroAnn
    }
    def scalacPluginIvyDeps = T {
      if (scalaVersion().startsWith("2.12."))
        Agg(Deps.macroParadise)
      else
        Agg.empty
    }

    object test extends Tests with TestCrossSources {
      def ivyDeps       = Agg(Deps.utest)
      def testFramework = "utest.runner.Framework"
    }
  }
}

object cats extends Module {
  object jvm extends Cross[CatsJvm](Versions.scala)
  object js  extends Cross[CatsJs](Versions.scala)

  trait CatsJvm extends Cats with MimaChecks {
    def moduleDeps = Seq(core.jvm())
  }
  trait CatsJs extends Cats with CaseAppScalaJsModule with MimaChecks {
    def moduleDeps = Seq(core.js())
  }

  trait Cats extends CrossSbtModule with CrossSources with CaseAppPublishModule {
    def ivyDeps = Agg(Deps.catsEffect)

    object test extends Tests with TestCrossSources {
      def ivyDeps       = Agg(Deps.utest)
      def testFramework = "utest.runner.Framework"
    }
  }
}

object cats2 extends Module {
  object jvm extends Cross[Cats2Jvm](Versions.scala)
  object js  extends Cross[Cats2Js](Versions.scala)

  trait Cats2Jvm extends Cats2 with MimaChecks {
    def moduleDeps = Seq(core.jvm())
    def sources    = cats.jvm().sources()
  }
  trait Cats2Js extends Cats2 with CaseAppScalaJsModule with MimaChecks {
    def moduleDeps = Seq(core.js())
    def sources    = cats.js().sources()
  }

  trait Cats2 extends CrossSbtModule with CrossSources with CaseAppPublishModule {
    def ivyDeps = Agg(Deps.catsEffect2)

    object test extends Tests with TestCrossSources {
      def ivyDeps       = Agg(Deps.utest)
      def testFramework = "utest.runner.Framework"
    }
  }
}

object tests extends Module {
  object jvm    extends Cross[TestsJvm](Versions.scala)
  object js     extends Cross[TestsJs](Versions.scala)
  object native extends Cross[TestsNative](Versions.scala)

  trait TestsJvm extends Tests0 {
    def moduleDeps = Seq(core.jvm())
  }
  trait TestsJs extends Tests0 with CaseAppScalaJsModule {
    def moduleDeps = Seq(core.js())
  }
  trait TestsNative extends Tests0 with CaseAppScalaNativeModule {
    def moduleDeps = Seq(core.native())
  }

  trait Tests0 extends CrossSbtModule with CrossSources {
    def ivyDeps = T {
      val maybeDataClass =
        if (scalaVersion().startsWith("2.")) Agg(Deps.dataClass)
        else Agg.empty
      Agg(Deps.sourcecode) ++ maybeDataClass
    }
    /*def scalacOptions = T {
      val maybeMacroAnn =
        if (scalaVersion().startsWith("2.13."))
          Seq("-Ymacro-annotations")
        else
          Nil
      super.scalacOptions() ++ maybeMacroAnn
    }
    def scalacPluginIvyDeps = T {
      if (scalaVersion().startsWith("2.12."))
        Agg(Deps.macroParadise)
      else
        Agg.empty
    }*/

    object test extends Tests with TestCrossSources {
      def ivyDeps = Agg(
        Deps.pprint,
        Deps.utest
      )
      def testFramework = "utest.runner.Framework"
    }
  }
}

trait CaseAppScalaJsModule extends ScalaJSModule {
  def scalaJSVersion = Versions.scalaJs
}

trait CaseAppScalaNativeModule extends ScalaNativeModule {
  def scalaNativeVersion = Versions.scalaNative
}

trait CaseAppPublishModule extends PublishModule {
  import CaseAppPublishModule._
  import mill.scalalib.publish._
  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "com.github.alexarchambault",
    url = "https://github.com/alexarchambault/case-app",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("alexarchambault", "case-app"),
    developers = Seq(
      Developer("alexarchambault", "Alex Archambault", "https://github.com/alexarchambault")
    )
  )
  def publishVersion = T(buildVersion)
}

object CaseAppPublishModule {
  private lazy val latestTaggedVersion =
    os.proc("git", "describe", "--abbrev=0", "--tags", "--match", "v*")
      .call().out
      .trim()
  private lazy val buildVersion = {
    val gitHead = os.proc("git", "rev-parse", "HEAD").call().out.trim()
    val maybeExactTag = scala.util.Try {
      os.proc("git", "describe", "--exact-match", "--tags", "--always", gitHead)
        .call().out
        .trim()
        .stripPrefix("v")
    }
    maybeExactTag.toOption.getOrElse {
      val commitsSinceTaggedVersion =
        os.proc("git", "rev-list", gitHead, "--not", latestTaggedVersion, "--count")
          .call().out.trim()
          .toInt
      val gitHash = os.proc("git", "rev-parse", "--short", "HEAD").call().out.trim()
      s"${latestTaggedVersion.stripPrefix("v")}-$commitsSinceTaggedVersion-$gitHash-SNAPSHOT"
    }
  }
}

object CrossSources {
  def extraSourcesDirs(sv: String, millSourcePath: os.Path, scope: String): Seq[PathRef] = {
    val (maj, majMin) = sv.split('.') match {
      case Array(maj0, min, _*) => (maj0, s"$maj0.$min")
      case _                    => sys.error(s"Malformed Scala version: $sv")
    }
    val baseDir = millSourcePath / os.up / "shared" / "src" / scope
    Seq(
      PathRef(baseDir / "scala"),
      PathRef(baseDir / s"scala-$maj"),
      PathRef(baseDir / s"scala-$majMin")
    )
  }
}

trait CrossSources extends SbtModule {
  import CrossSources._
  def sources = T.sources {
    val sv = scalaVersion()
    super.sources() ++ extraSourcesDirs(sv, millSourcePath, "main")
  }
}

trait TestCrossSources extends SbtModule {
  import CrossSources._
  def sources = T.sources {
    val sv = scalaVersion()
    super.sources() ++ extraSourcesDirs(sv, millSourcePath, "test")
  }
}

trait MimaChecks extends Mima {

  def mimaPreviousVersions = T {
    os.proc(
      "git",
      "tag",
      "--merged",
      "HEAD^",
      "--contains",
      "8e0544e5ce1f9ce1dd3bd85308332b5dd1cd4985"
    )
      .call()
      .out.lines()
      .map(_.trim)
      .filter(_.startsWith("v"))
      .map(_.stripPrefix("v"))
  }

  def mimaPreviousArtifacts = T {
    val versions = mimaPreviousVersions().distinct
    mill.api.Result.Success(
      Agg.from(
        versions.map(version =>
          ivy"${pomSettings().organization}:${artifactId()}:$version"
        )
      )
    )
  }
}

object ci extends Module {

  def publishSonatype(tasks: mill.main.Tasks[PublishModule.PublishData]) = T.command {
    publishSonatype0(
      data = define.Target.sequence(tasks.value)(),
      log = T.ctx().log
    )
  }

  def publishSonatype0(
    data: Seq[PublishModule.PublishData],
    log: mill.api.Logger
  ): Unit = {

    val credentials = sys.env("SONATYPE_USERNAME") + ":" + sys.env("SONATYPE_PASSWORD")
    val pgpPassword = sys.env("PGP_PASSPHRASE")
    val timeout     = 10.minutes

    val artifacts = data.map {
      case PublishModule.PublishData(a, s) =>
        (s.map { case (p, f) => (p.path, f) }, a)
    }

    val isRelease = {
      val versions = artifacts.map(_._2.version).toSet
      val set      = versions.map(!_.endsWith("-SNAPSHOT"))
      assert(
        set.size == 1,
        s"Found both snapshot and non-snapshot versions: ${versions.toVector.sorted.mkString(", ")}"
      )
      set.head
    }
    val publisher = new scalalib.publish.SonatypePublisher(
      uri = "https://oss.sonatype.org/service/local",
      snapshotUri = "https://oss.sonatype.org/content/repositories/snapshots",
      credentials = credentials,
      signed = true,
      // format: off
      gpgArgs = Seq(
        "--detach-sign",
        "--batch=true",
        "--yes",
        "--pinentry-mode", "loopback",
        "--passphrase", pgpPassword,
        "--armor",
        "--use-agent"
      ),
      // format: on
      readTimeout = timeout.toMillis.toInt,
      connectTimeout = timeout.toMillis.toInt,
      log = log,
      awaitTimeout = timeout.toMillis.toInt,
      stagingRelease = isRelease
    )

    publisher.publishAll(isRelease, artifacts: _*)
  }

}
