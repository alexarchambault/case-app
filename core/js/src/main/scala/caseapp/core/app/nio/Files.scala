package caseapp.core.app.nio

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

object Files {

  def exists(path: Path): Boolean =
    nodeFs.existsSync(path.underlying).asInstanceOf[Boolean]
  def createDirectories(path: Path): Unit =
    nodeFs.mkdirSync(path.underlying, js.Dictionary("recursive" -> true))
  def isRegularFile(path: Path): Boolean =
    exists(path) && nodeFs.statSync(path.underlying).isFile().asInstanceOf[Boolean]
  def deleteIfExists(path: Path): Boolean = {
    nodeFs.rmSync(path.underlying, js.Dictionary("recursive" -> true))
    true
  }

  private lazy val nodeFs = g.require("fs")
}
