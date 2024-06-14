package caseapp.core.app

import caseapp.core.complete.{
  Bash,
  CompletionItem,
  CompletionsInstallOptions,
  CompletionsUninstallOptions,
  Fish,
  Zsh
}

import java.io.File
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import java.util.Arrays

trait PlatformCommandsMethods { self: CommandsEntryPoint =>
  protected def writeCompletions(script: String, dest: String): Unit = {
    val destPath = Paths.get(dest)
    Files.write(destPath, script.getBytes(StandardCharsets.UTF_8))
  }

  protected def completeMainHook(args: Array[String]): Unit =
    for (path <- completionDebugFile) {
      val output = s"completeMain(${args.toSeq})"
      Files.write(path, output.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND)
    }

  def completionsInstalledMessage(
    rcFile: String,
    updated: Boolean
  ): Iterator[String] = {
    val q = "\""
    val evalCommand =
      s"eval $q$$($progName ${completionsCommandName.mkString(" ")} install --env)$q"
    if (updated)
      Iterator(
        s"Updated $rcFile",
        "",
        s"It is recommended to reload your shell, or source $rcFile in the " +
          "current session, for its changes to be taken into account.",
        "",
        "Alternatively, enable completions in the current session with",
        "",
        s"  $evalCommand",
        ""
      )
    else
      Iterator(
        s"$rcFile already up-to-date.",
        "",
        "If needed, enable completions in the current session with",
        "",
        s"  $evalCommand",
        ""
      )
  }

  def shell: Option[String]           = Option(System.getenv("SHELL"))
  def completionHome: Path            = Paths.get(sys.props("user.home"))
  def completionXdgHome: Option[Path] = Option(System.getenv("XDG_CONFIG_HOME")).map(Paths.get(_))
  def completionZDotDir: Option[Path] = Option(System.getenv("ZDOTDIR")).map(Paths.get(_))
  def completionDebugFile: Option[Path] =
    Option(System.getenv("CASEAPP_COMPLETION_DEBUG")).map(Paths.get(_))

  private def fishRcFile(name: String): Path =
    completionXdgHome
      .getOrElse(completionHome.resolve(".config"))
      .resolve("fish")
      .resolve("completions")
      .resolve(s"$name.fish")

  private def zshrcFile: Path =
    completionZDotDir
      .getOrElse(completionHome)
      .resolve(".zshrc")
  private def bashrcFile: Path =
    completionHome.resolve(".bashrc")

  private def zshCompletionWorkingDir(forcedOutputDir: Option[String]): Path =
    forcedOutputDir
      .orElse(completionsWorkingDirectory)
      .map(Paths.get(_).resolve("zsh"))
      .getOrElse {
        val zDotDir = completionZDotDir.getOrElse(completionHome)
        completionXdgHome
          .getOrElse(zDotDir.resolve(".config"))
          .resolve("zsh")
          .resolve("completions")
      }

  // Adapted from https://github.com/VirtusLab/scala-cli/blob/eced0b35c769eca58ae6f1b1a3be0f29a8700859/modules/cli/src/main/scala/scala/cli/commands/installcompletions/InstallCompletions.scala
  def completionsInstall(
    completionsWorkingDirectory: Option[String],
    options: CompletionsInstallOptions
  ): Unit = {
    val name = options.name.getOrElse(Paths.get(progName).getFileName.toString)
    val format = PlatformCommandsMethods.getFormat(options.format, shell).getOrElse {
      printLine(
        "Cannot determine current shell, pass the shell you use with --shell, like",
        toStderr = true
      )
      printLine("", toStderr = true)
      for (shell <- Seq(Bash.shellName, Zsh.shellName, Fish.shellName))
        printLine(
          s"  $name ${completionsCommandName.mkString(" ")} install --shell $shell",
          toStderr = true
        )
      printLine("", toStderr = true)
      exit(1)
    }

    val (rcScript, defaultRcFile) = format match {
      case Bash.id | Bash.shellName =>
        (Bash.script(name), bashrcFile)
      case Fish.id | Fish.shellName =>
        (Fish.script(name), fishRcFile(name))
      case Zsh.id | Zsh.shellName =>
        val completionScript     = Zsh.script(name)
        val dir                  = zshCompletionWorkingDir(options.output)
        val completionScriptDest = dir.resolve(s"_$name")
        val needsWrite =
          !Files.exists(completionScriptDest) ||
          new String(
            Files.readAllBytes(completionScriptDest),
            StandardCharsets.UTF_8
          ) != completionScript
        if (needsWrite) {
          printLine(s"Writing $completionScriptDest")
          Files.createDirectories(completionScriptDest.getParent)
          Files.write(completionScriptDest, completionScript.getBytes(StandardCharsets.UTF_8))
        }
        val script = Seq(s"""fpath=("$dir" $$fpath)""", "compinit")
          .map(_ + System.lineSeparator())
          .mkString
        (script, zshrcFile)
      case _ =>
        printLine(s"Unrecognized or unsupported shell: $format", toStderr = true)
        exit(1)
    }

    if (options.env)
      println(rcScript)
    else {
      val rcFile = format match {
        case Fish.id | Fish.shellName =>
          options.output.map(Paths.get(_)).map(_.resolve(s"$name.fish")).getOrElse(defaultRcFile)
        case _ =>
          options.rcFile.map(Paths.get(_)).getOrElse(defaultRcFile)
      }
      val banner = options.banner.replace("{NAME}", name)
      val updated = ProfileFileUpdater.addToProfileFile(
        rcFile,
        banner,
        rcScript,
        Charset.defaultCharset()
      )

      for (line <- completionsInstalledMessage(rcFile.toString, updated))
        printLine(line, toStderr = true)
    }
  }

  def completionsUninstall(
    completionsWorkingDirectory: Option[String],
    options: CompletionsUninstallOptions
  ): Unit = {
    val name = options.name.getOrElse(Paths.get(progName).getFileName.toString)

    val rcFiles = options.rcFile
      .map(file => Seq(Paths.get(file)))
      .getOrElse(Seq(zshrcFile, bashrcFile))
      .filter(Files.exists(_))

    val maybeDelete = Seq(
      zshCompletionWorkingDir(options.output).resolve(s"_$name"),
      fishRcFile(name)
    )

    for (rcFile <- rcFiles) {
      val banner = options.banner.replace("{NAME}", name)

      val updated = ProfileFileUpdater.removeFromProfileFile(
        rcFile,
        banner,
        Charset.defaultCharset()
      )

      if (updated) {
        printLine(s"Updated $rcFile", toStderr = true)
        printLine(s"$name completions uninstalled successfully", toStderr = true)
      }
      else
        printLine(s"No $name completion section found in $rcFile", toStderr = true)
    }

    for (f <- maybeDelete)
      if (Files.isRegularFile(f)) {
        val deleted = Files.deleteIfExists(f)
        if (deleted)
          printLine(s"Removed $f", toStderr = true)
      }
  }

}

object PlatformCommandsMethods {
  def getFormat(format: Option[String], shellOpt: Option[String]): Option[String] =
    format.map(_.trim).filter(_.nonEmpty)
      .orElse {
        shellOpt.map(_.split(File.separator).last).map {
          case Bash.shellName => Bash.id
          case Fish.shellName => Fish.id
          case Zsh.shellName  => Zsh.id
          case other          => other
        }
      }
}
