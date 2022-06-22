import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.plugin.MimaPlugin
import sbt._
import sbt.Keys._

import scala.sys.process._

object Mima {

  def binaryCompatibilityVersions: Set[String] =
    Seq("git", "tag", "--merged", "HEAD^", "--contains", "4d7dd0e183252625798341a9b0694dbbc41b31e0")
      .!!
      .linesIterator
      .map(_.trim)
      .filter(_ != "v2.1.0-M14")
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
