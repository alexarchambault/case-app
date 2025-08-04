package caseapp.core.app

import caseapp.core.app.nio._
import caseapp.core.commandparser.RuntimeCommandParser
import caseapp.core.complete.{
  Bash,
  CompletionItem,
  CompletionsInstallOptions,
  CompletionsUninstallOptions,
  Fish,
  Zsh
}
import caseapp.core.help.{Help, HelpFormat, RuntimeCommandHelp, RuntimeCommandsHelp}

abstract class CommandsEntryPoint {

  def defaultCommand: Option[Command[_]] = None
  def commands: Seq[Command[_]]

  def progName: String
  def description: String = ""
  def summaryDesc: String = ""

  def help: RuntimeCommandsHelp =
    RuntimeCommandsHelp(
      progName,
      Some(description).filter(_.nonEmpty),
      defaultCommand.map(_.finalHelp: Help[_]).getOrElse(Help[Unit]()),
      commands.map(cmd => RuntimeCommandHelp(cmd.names, cmd.finalHelp, cmd.group, cmd.hidden)),
      Some(summaryDesc).filter(_.nonEmpty)
    )

  def helpFormat: HelpFormat =
    HelpFormat.default()

  private def commandProgName(commandName: List[String]): String =
    (progName +: commandName).mkString(" ")

  def enableCompleteCommand: Boolean    = false
  def completeCommandName: List[String] = List("complete")

  def enableCompletionsCommand: Boolean             = false
  def completionsCommandName: List[String]          = List("completions")
  def completionsCommandAliases: List[List[String]] = List(
    completionsCommandName,
    List("completion")
  )

  def completionsPrintInstructions(): Unit = {
    printLine("To install completions, run", toStderr = true)
    printLine("", toStderr = true)
    printLine(
      s"  $progName ${completionsCommandName.mkString(" ")} install",
      toStderr = true
    )
    printLine("", toStderr = true)
  }

  def completePrintInstructions(toStderr: Boolean): Unit = {
    val formats = Seq(Bash.id, Zsh.id, Fish.id)
    printLine("To manually get completions, run", toStderr = toStderr)
    printLine("", toStderr = toStderr)
    printLine(
      s"  $progName ${completeCommandName.mkString(" ")} ${formats.mkString("|")} index command...",
      toStderr = toStderr
    )
    printLine("", toStderr = toStderr)
    printLine(
      "where index starts from one, and command... includes the command name, like",
      toStderr = toStderr
    )
    printLine("", toStderr = toStderr)
    printLine(
      s"  $progName ${completeCommandName.mkString(" ")} ${Zsh.id} 2 $progName --",
      toStderr = toStderr
    )
    printLine("", toStderr = toStderr)
    printLine("to get completions for '--'", toStderr = toStderr)
  }

  def completionsPrintUsage(): Nothing = {
    completionsPrintInstructions()
    exit(1)
  }

  def completeUnrecognizedFormat(format: String): Nothing = {
    printLine(s"Unrecognized completion format '$format'", toStderr = true)
    exit(1)
  }

  def completionsWorkingDirectory: Option[String] = None

  protected def completeMainHook(args: Array[String]): Unit =
    for (path <- completionDebugFile) {
      val output = s"completeMain(${args.toSeq})"
      FileOps.appendToFile(path, output)
    }

  def completionsInstalledMessage(
    rcFile: String,
    updated: Boolean
  ): Iterator[String] = {
    val q           = "\""
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

  def shell: Option[String]             = FileOps.readEnv("SHELL")
  def completionHome: Path              = FileOps.homeDir
  def completionXdgHome: Option[Path]   = FileOps.readEnv("XDG_CONFIG_HOME").map(Paths.get(_))
  def completionZDotDir: Option[Path]   = FileOps.readEnv("ZDOTDIR").map(Paths.get(_))
  def completionDebugFile: Option[Path] =
    FileOps.readEnv("CASEAPP_COMPLETION_DEBUG").map(Paths.get(_))

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
    val name   = options.name.getOrElse(Paths.get(progName).getFileName.toString)
    val format = CommandsEntryPoint.getFormat(options.format, shell, File.separator).getOrElse {
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
        val needsWrite           = !FileOps.exists(completionScriptDest) ||
          FileOps.readFile(completionScriptDest) != completionScript
        if (needsWrite) {
          printLine(s"Writing $completionScriptDest")
          FileOps.createDirectories(completionScriptDest.getParent)
          FileOps.writeFile(completionScriptDest, completionScript)
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
      val banner  = options.banner.replace("{NAME}", name)
      val updated = ProfileFileUpdater.addToProfileFile(rcFile, banner, rcScript)

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
      .filter(FileOps.exists(_))

    val maybeDelete = Seq(
      zshCompletionWorkingDir(options.output).resolve(s"_$name"),
      fishRcFile(name)
    )

    for (rcFile <- rcFiles) {
      val banner = options.banner.replace("{NAME}", name)

      val updated = ProfileFileUpdater.removeFromProfileFile(rcFile, banner)

      if (updated) {
        printLine(s"Updated $rcFile", toStderr = true)
        printLine(s"$name completions uninstalled successfully", toStderr = true)
      }
      else
        printLine(s"No $name completion section found in $rcFile", toStderr = true)
    }

    for (f <- maybeDelete)
      if (FileOps.isRegularFile(f)) {
        val deleted = FileOps.deleteIfExists(f)
        if (deleted)
          printLine(s"Removed $f", toStderr = true)
      }
  }

  def completionsMain(args: Array[String]): Unit = {

    def script(format: String): String =
      format match {
        case Bash.shellName | Bash.id => Bash.script(progName)
        case Fish.shellName | Fish.id => Fish.script(progName)
        case Zsh.shellName | Zsh.id   => Zsh.script(progName)
        case _                        =>
          completeUnrecognizedFormat(format)
      }

    args match {
      case Array("install", args0 @ _*) =>
        val (options, rem) = CaseApp.process[CompletionsInstallOptions](args0)
        if (rem.all.nonEmpty)
          sys.error(s"Unexpected arguments passed to completions install: ${rem.all.mkString(" ")}")
        completionsInstall(completionsWorkingDirectory, options)
      case Array("uninstall", args0 @ _*) =>
        val (options, rem) = CaseApp.process[CompletionsUninstallOptions](args0)
        completionsUninstall(completionsWorkingDirectory, options)
      case Array(format, dest) =>
        val script0 = script(format)
        FileOps.writeFile(Paths.get(dest), script0)
      case Array(format) =>
        val script0 = script(format)
        printLine(script0)
      case _ =>
        completionsPrintUsage()
    }
  }

  def complete(args: Seq[String], index: Int): List[CompletionItem] =
    defaultCommand match {
      case None =>
        RuntimeCommandParser.complete(commands, args.toList, index)
      case Some(defaultCommand0) =>
        RuntimeCommandParser.complete(defaultCommand0, commands, args.toList, index)
    }

  def completeMain(args: Array[String]): Unit = {
    completeMainHook(args)
    args match {
      case Array(format, indexStr, userArgs @ _*) =>
        val index          = indexStr.toInt - 2 // -1 for argv[0], and -1 as indices start at 1
        val prefix: String = userArgs.applyOrElse(index + 1, (_: Int) => "")
        val items          = complete(userArgs.toList.drop(1), index)
          .flatMap { item =>
            val values = item.values.filter(_.startsWith(prefix))
            if (values.isEmpty) Nil
            else Seq(CompletionItem(values.head, item.description, values.tail.toList))
          }
        format match {
          case Bash.id =>
            printLine(Bash.print(items))
          case Fish.id =>
            printLine(Fish.print(items))
          case Zsh.id =>
            printLine(Zsh.print(items))
          case _ =>
            completeUnrecognizedFormat(format)
        }
      case Array("--help" | "--usage" | "-h") =>
        completePrintInstructions(toStderr = false)
      case _ =>
        completePrintInstructions(toStderr = true)
        exit(1)
    }
  }

  def exit(code: Int): Nothing =
    PlatformUtil.exit(code)
  def printLine(line: String, toStderr: Boolean): Unit =
    if (toStderr)
      System.err.println(line)
    else
      println(line)
  final def printLine(line: String): Unit =
    printLine(line, toStderr = false)

  def printUsage(): Nothing = {
    val usage = help.help(helpFormat, showHidden = false)
    printLine(usage)
    exit(0)
  }

  def main(args: Array[String]): Unit = {
    val actualArgs = PlatformUtil.arguments(args)
    if (enableCompleteCommand && actualArgs.startsWith(completeCommandName.toArray[String]))
      completeMain(actualArgs.drop(completeCommandName.length))
    else {
      val completionAliasOpt =
        if (enableCompletionsCommand) completionsCommandAliases.find(actualArgs.startsWith(_))
        else None
      completionAliasOpt match {
        case Some(completionAlias) =>
          completionsMain(actualArgs.drop(completionAlias.length))
        case None =>
          defaultCommand match {
            case None =>
              RuntimeCommandParser.parse(commands, actualArgs.toList) match {
                case None =>
                  printUsage()
                case Some((commandName, command, commandArgs)) =>
                  command.main(commandProgName(commandName), commandArgs.toArray)
              }
            case Some(defaultCommand0) =>
              val (commandName, command, commandArgs) =
                RuntimeCommandParser.parse(defaultCommand0, commands, actualArgs.toList)
              command.main(commandProgName(commandName), commandArgs.toArray)
          }
      }
    }
  }
}

object CommandsEntryPoint {
  def getFormat(format: Option[String], shellOpt: Option[String], fileSep: String): Option[String] =
    format.map(_.trim).filter(_.nonEmpty)
      .orElse {
        shellOpt.map(_.split(fileSep).last).map {
          case Bash.shellName => Bash.id
          case Fish.shellName => Fish.id
          case Zsh.shellName  => Zsh.id
          case other          => other
        }
      }
}
