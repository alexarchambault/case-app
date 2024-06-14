package caseapp.core.app

import caseapp.core.complete.{CompletionsInstallOptions, CompletionsUninstallOptions}

import scala.scalajs.js.Dynamic.{global => g}

trait PlatformCommandsMethods { self: CommandsEntryPoint =>
  private lazy val fs = g.require("fs")
  protected def writeCompletions(script: String, dest: String): Unit =
    fs.writeFileSync(dest, script)
  protected def completeMainHook(args: Array[String]): Unit = ()

  def completionsInstall(completionsWorkingDirectory: Option[String], args: CompletionsInstallOptions): Unit = {
    printLine("Completion installation not available on Scala.js")
    exit(1)
  }

  def completionsUninstall(completionsWorkingDirectory: Option[String], args: CompletionsUninstallOptions): Unit = {
    printLine("Completion uninstallation not available on Scala.js")
    exit(1)
  }
}
