package caseapp.core.app.nio

import java.nio.charset.StandardCharsets
import java.nio.file.{FileAlreadyExistsException, Files, Path, Paths, StandardOpenOption}

object FileOps {

  def readFile(path: Path): String =
    new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
  def writeFile(path: Path, content: String): Unit =
    Files.write(path, content.getBytes(StandardCharsets.UTF_8))
  def appendToFile(path: Path, content: String): Unit =
    Files.write(path, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND)
  def readEnv(varName: String): Option[String] =
    Option(System.getenv(varName))
  def homeDir: Path =
    Paths.get(sys.props("user.home"))

  def createDirectories(path: Path): Unit =
    try Files.createDirectories(path)
    catch {
      // Ignored, see https://bugs.openjdk.java.net/browse/JDK-8130464
      case _: FileAlreadyExistsException if Files.isDirectory(path) =>
    }

  // simple aliases, to avoid explicit imports of Files,
  // which might point to _root_.java.nio.file.Files or _root_.caseapp.core.app.nio.Files
  def exists(path: Path): Boolean =
    Files.exists(path)
  def isRegularFile(path: Path): Boolean =
    Files.isRegularFile(path)
  def deleteIfExists(path: Path): Boolean =
    Files.deleteIfExists(path)
}
