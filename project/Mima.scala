
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt._
import sbt.Keys._

import scala.sys.process._

object Mima {

  def binaryCompatibilityVersions: Set[String] =
    Seq("git", "tag", "--merged", "HEAD^", "--contains", "1b90b2ac934eb41d6a82d10bd78561fa91fb4ead")
      .!!
      .linesIterator
      .map(_.trim)
      .filter(_.startsWith("v"))
      .map(_.stripPrefix("v"))
      .filter(_ != "2.1.0-M5")
      .toSet

  def settings = Def.settings(
    MimaPlugin.autoImport.mimaPreviousArtifacts := {
      binaryCompatibilityVersions
        .map { ver =>
          (organization.value % moduleName.value % ver)
            .cross(crossVersion.value)
        }
    }
  )

}
