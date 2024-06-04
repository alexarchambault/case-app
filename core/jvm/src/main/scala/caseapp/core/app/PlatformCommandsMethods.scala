package caseapp.core.app

import caseapp.core.complete.{Bash, Fish, Zsh}

import java.io.File
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util.Arrays

trait PlatformCommandsMethods { self: CommandsEntryPoint =>
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

  // Adapted from https://github.com/VirtusLab/scala-cli/blob/eced0b35c769eca58ae6f1b1a3be0f29a8700859/modules/cli/src/main/scala/scala/cli/commands/installcompletions/InstallCompletions.scala
  def completionsInstall(completionsWorkingDirectory: String, args: Seq[String]): Unit = {
    val (options, rem) = CaseApp.process[PlatformCommandsMethods.CompletionsInstallOptions](args)
    lazy val completionsDir = Paths.get(options.output.getOrElse(completionsWorkingDirectory))

    val name = options.name.getOrElse(Paths.get(progName).getFileName.toString)
    val format = PlatformCommandsMethods.getFormat(options.format).getOrElse {
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
        val script        = Bash.script(name)
        val defaultRcFile = Paths.get(sys.props("user.home")).resolve(".bashrc")
        (script, defaultRcFile)
      case Fish.id | Fish.shellName =>
        val script = Fish.script(name)
        val defaultRcFile =
          Option(System.getenv("XDG_CONFIG_HOME")).map(Paths.get(_))
            .getOrElse(Paths.get(sys.props("user.home"), ".config"))
            .resolve("fish")
            .resolve("completions")
            .resolve(s"$name.fish")
        (script, defaultRcFile)
      case Zsh.id | Zsh.shellName =>
        val completionScript = Zsh.script(name)
        val zDotDir = Paths.get(Option(System.getenv("ZDOTDIR")).getOrElse(sys.props("user.home")))
        val defaultRcFile        = zDotDir.resolve(".zshrc")
        val dir                  = completionsDir.resolve("zsh")
        val completionScriptDest = dir.resolve(s"_$name")
        val content              = completionScript.getBytes(Charset.defaultCharset())
        val needsWrite = !Files.exists(completionScriptDest) ||
          !Arrays.equals(Files.readAllBytes(completionScriptDest), content)
        if (needsWrite) {
          printLine(s"Writing $completionScriptDest")
          Files.createDirectories(completionScriptDest.getParent)
          Files.write(completionScriptDest, content)
        }
        val script = Seq(
          s"""fpath=("$dir" $$fpath)""",
          "compinit"
        ).map(_ + System.lineSeparator()).mkString
        (script, defaultRcFile)
      case _ =>
        printLine(s"Unrecognized or unsupported shell: $format", toStderr = true)
        exit(1)
    }

    if (options.env)
      println(rcScript)
    else {
      val rcFile = format match {
        case Fish.id | Fish.shellName =>
          options.output.map(Paths.get(_, s"$name.fish")).getOrElse(defaultRcFile)
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

      val q = "\""
      val evalCommand =
        s"eval $q$$($progName ${completionsCommandName.mkString(" ")} install --env)$q"
      if (updated) {
        printLine(s"Updated $rcFile", toStderr = true)
        printLine("", toStderr = true)
        printLine(
          s"It is recommended to reload your shell, or source $rcFile in the " +
            "current session, for its changes to be taken into account.",
          toStderr = true
        )
        printLine("", toStderr = true)
        printLine(
          "Alternatively, enable completions in the current session with",
          toStderr = true
        )
        printLine("", toStderr = true)
        printLine(s"  $evalCommand", toStderr = true)
        printLine("", toStderr = true)
      }
      else {
        printLine(s"$rcFile already up-to-date.", toStderr = true)
        printLine("", toStderr = true)
        printLine("If needed, enable completions in the current session with", toStderr = true)
        printLine("", toStderr = true)
        printLine(s"  $evalCommand", toStderr = true)
        printLine("", toStderr = true)
      }
    }
  }

  def completionsUninstall(completionsWorkingDirectory: String, args: Seq[String]): Unit = {
    val (options, rem) = CaseApp.process[PlatformCommandsMethods.CompletionsUninstallOptions](args)
    val name = options.name.getOrElse(Paths.get(progName).getFileName.toString)

    val home    = Paths.get(sys.props("user.home"))
    val zDotDir = Option(System.getenv("ZDOTDIR")).map(Paths.get(_)).getOrElse(home)
    val fishCompletionsDir = options.output.map(Paths.get(_))
      .getOrElse(sys.env.get("XDG_CONFIG_HOME").map(Paths.get(_)).getOrElse(home)
        .resolve("fish")
        .resolve("completions"))
    val rcFiles = options.rcFile.map(file => Seq(Paths.get(file))).getOrElse(Seq(
      zDotDir.resolve(".zshrc"),
      home.resolve(".bashrc"),
      fishCompletionsDir.resolve(s"$name.fish")
    )).filter(Files.exists(_))

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
  }
}

object PlatformCommandsMethods {
  import caseapp.{HelpMessage, Name}
  import caseapp.core.help.Help
  import caseapp.core.parser.Parser

  // from https://github.com/VirtusLab/scala-cli/blob/eced0b35c769eca58ae6f1b1a3be0f29a8700859/modules/cli/src/main/scala/scala/cli/commands/installcompletions/InstallCompletionsOptions.scala
  // format: off
  final case class CompletionsInstallOptions(
    @HelpMessage("Print completions to stdout")
      env: Boolean = false,
    @HelpMessage("Custom completions name")
      name: Option[String] = None,
    @HelpMessage("Name of the shell, either zsh, fish or bash")
    @Name("shell")
      format: Option[String] = None,
    @HelpMessage("Completions output directory (defaults to $XDG_CONFIG_HOME/fish/completions on fish)")
    @Name("o")
      output: Option[String] = None,
    @HelpMessage("Custom banner in comment placed in rc file (bash or zsh only)")
      banner: String = "{NAME} completions",
    @HelpMessage("Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell (bash or zsh only)")
      rcFile: Option[String] = None
  )
  // format: on

  object CompletionsInstallOptions {
    implicit lazy val parser: Parser[CompletionsInstallOptions] = Parser.derive
    implicit lazy val help: Help[CompletionsInstallOptions]     = Help.derive
  }

  // from https://github.com/VirtusLab/scala-cli/blob/eced0b35c769eca58ae6f1b1a3be0f29a8700859/modules/cli/src/main/scala/scala/cli/commands/uninstallcompletions/SharedUninstallCompletionsOptions.scala
  // format: off
  final case class CompletionsUninstallOptions(
    @HelpMessage("Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell (bash or zsh only)")
      rcFile: Option[String] = None,
    @HelpMessage("Custom banner in comment placed in rc file")
      banner: String = "{NAME} completions",
    @HelpMessage("Custom completions name")
      name: Option[String] = None,
    @HelpMessage("Completions output directory (defaults to $XDG_CONFIG_HOME/fish/completions on fish)")
    @Name("o")
      output: Option[String] = None,
  )
  // format: on

  object CompletionsUninstallOptions {
    implicit lazy val parser: Parser[CompletionsUninstallOptions] = Parser.derive
    implicit lazy val help: Help[CompletionsUninstallOptions]     = Help.derive
  }

  def getFormat(format: Option[String]): Option[String] =
    format.map(_.trim).filter(_.nonEmpty)
      .orElse {
        Option(System.getenv("SHELL")).map(_.split(File.separator).last).map {
          case Bash.shellName => Bash.id
          case Fish.shellName => Fish.id
          case Zsh.shellName  => Zsh.id
          case other          => other
        }
      }
}
