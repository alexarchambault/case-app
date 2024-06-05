package caseapp.core.app

import scala.scalajs.js.Dynamic.{global => g}

trait PlatformCommandsMethods { self: CommandsEntryPoint =>
  private lazy val fs = g.require("fs")
  protected def writeCompletions(script: String, dest: String): Unit =
    fs.writeFileSync(dest, script)
  protected def completeMainHook(args: Array[String]): Unit = ()

  def completionsInstall(completionsWorkingDirectory: String, args: Seq[String]): Unit = {
    printLine("Completion installation not available on Scala.js")
    exit(1)
  }

  def completionsUninstall(completionsWorkingDirectory: String, args: Seq[String]): Unit = {
    printLine("Completion uninstallation not available on Scala.js")
    exit(1)
  }
}
