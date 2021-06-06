package caseapp.core.app

import scala.scalajs.js.Dynamic.{global => g}

trait PlatformCommandsMethods {
  private lazy val fs = g.require("fs")
  protected def writeCompletions(script: String, dest: String): Unit =
    fs.writeFileSync(dest, script)
}
