package caseapp.core.app.nio

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

object FileOps {

  private lazy val nodeFs      = g.require("fs")
  private lazy val nodeOs      = g.require("os")
  private lazy val nodeProcess = g.require("process")

  def readFile(path: Path): String =
    nodeFs.readFileSync(path.underlying, js.Dictionary("encoding" -> "utf8")).asInstanceOf[String]
  def writeFile(path: Path, content: String): Unit =
    nodeFs.writeFileSync(path.underlying, content)
  def appendToFile(path: Path, content: String): Unit =
    nodeFs.writeFileSync(path.underlying, content, js.Dictionary("mode" -> "a"))
  def readEnv(varName: String): Option[String] =
    nodeProcess.env.asInstanceOf[js.Dictionary[String]].get(varName)
  def homeDir: Path =
    Paths.get(nodeOs.homedir().asInstanceOf[String])

  def createDirectories(path: Path): Unit =
    nodeFs.mkdirSync(path.underlying, js.Dictionary("recursive" -> true))

  // simple aliases, to avoid explicit imports of Files,
  // which might point to _root_.java.nio.file.Files or _root_.caseapp.core.app.nio.Files
  def exists(path: Path): Boolean =
    Files.exists(path)
  def isRegularFile(path: Path): Boolean =
    Files.isRegularFile(path)
  def deleteIfExists(path: Path): Boolean =
    Files.deleteIfExists(path)
}
