package caseapp.core.app

import scala.scalajs.js
import js.Dynamic.{global => g}

object PlatformUtil {
  private lazy val process     = g.require("process")
  def exit(code: Int): Nothing = {
    process.exit(code)
    sys.error(s"Attempt to exit with code $code failed")
  }
  def arguments(args: Array[String]): Array[String] =
    if (args.isEmpty)
      process.argv
        .asInstanceOf[js.Array[String]]
        .toArray
        .drop(2) // drop "node" and "/path/to/app.js"
    else
      args
}
