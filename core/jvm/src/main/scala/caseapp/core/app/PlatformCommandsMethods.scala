package caseapp.core.app

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

trait PlatformCommandsMethods {
  protected def writeCompletions(script: String, dest: String): Unit = {
    val destPath = Paths.get(dest)
    Files.write(destPath, script.getBytes(StandardCharsets.UTF_8))
  }
  protected def completeMainHook(args: Array[String]): Unit =
    Option(System.getenv("CASEAPP_COMPLETION_DEBUG")).foreach { pathStr =>
      val path   = Paths.get(pathStr)
      val output = s"completeMain(${args.toSeq})"
      Files.write(path, output.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND)
    }
}
