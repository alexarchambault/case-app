package caseapp.core.app

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

trait PlatformCommandsMethods {
  protected def writeCompletions(script: String, dest: String): Unit = {
    val destPath = Paths.get(dest)
    Files.write(destPath, script.getBytes(StandardCharsets.UTF_8))
  }
  protected def completeMainHook(args: Array[String]): Unit = ()
}
