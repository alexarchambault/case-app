import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt._
import sbt.Keys._

import scala.sys.process._

object Mima {

  def binaryCompatibilityVersions: Set[String] =
    Seq("git", "tag", "--merged", "HEAD^", "--contains", "5a653bdd41972587bd3fb3d9751c0e0cf11c1e74")
      .!!
      .linesIterator
      .map(_.trim)
      .filter(_.startsWith("v"))
      .map(_.stripPrefix("v"))
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
