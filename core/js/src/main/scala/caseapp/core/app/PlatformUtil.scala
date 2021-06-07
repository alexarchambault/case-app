package caseapp.core.app

import scala.scalajs.js.Dynamic.{global => g}

object PlatformUtil {
  private lazy val process = g.require("process")
  def exit(code: Int): Nothing = {
    process.exit(code)
    sys.error(s"Attempt to exit with code $code failed")
  }
}
