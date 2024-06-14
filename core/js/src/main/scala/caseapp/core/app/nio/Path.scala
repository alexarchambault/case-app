package caseapp.core.app.nio

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

final case class Path(underlying: String) {
  import Path.nodePath
  def resolve(chunk: String): Path =
    Path(nodePath.join(underlying, chunk).asInstanceOf[String])
  def getFileName: Path =
    Path(nodePath.basename(underlying).asInstanceOf[String])
  def getParent: Path =
    Path(nodePath.join(underlying, "..").asInstanceOf[String])
  override def toString: String = underlying
}

object Path {
  private[nio] lazy val nodePath = g.require("path")
}
