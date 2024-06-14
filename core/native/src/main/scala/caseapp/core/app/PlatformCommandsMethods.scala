package caseapp.core.app

import caseapp.core.complete.{CompletionsInstallOptions, CompletionsUninstallOptions}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

trait PlatformCommandsMethods { self: CommandsEntryPoint =>
  protected def writeCompletions(script: String, dest: String): Unit = {
    val destPath = Paths.get(dest)
    Files.write(destPath, script.getBytes(StandardCharsets.UTF_8))
  }
  protected def completeMainHook(args: Array[String]): Unit = ()

  def completionsInstall(completionsWorkingDirectory: Option[String], args: CompletionsInstallOptions): Unit = {
    // The JVM implementation might just work from here
    printLine("Completion installation not available on Scala Native")
    exit(1)
  }

  def completionsUninstall(completionsWorkingDirectory: Option[String], args: CompletionsUninstallOptions): Unit = {
    // The JVM implementation might just work from here
    printLine("Completion uninstallation not available on Scala Native")
    exit(1)
  }
}
