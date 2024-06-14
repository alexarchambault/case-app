package caseapp.core.app

import caseapp.core.commandparser.RuntimeCommandParser
import caseapp.core.complete.{Bash, CompletionItem, Fish, Zsh}
import caseapp.core.help.{Help, HelpFormat, RuntimeCommandHelp, RuntimeCommandsHelp}

abstract class CommandsEntryPoint extends PlatformCommandsMethods {

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

  def enableCompletionsCommand: Boolean    = false
  def completionsCommandName: List[String] = List("completions")
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

  def completionsMain(args: Array[String]): Unit = {

    def script(format: String): String =
      format match {
        case Bash.shellName | Bash.id => Bash.script(progName)
        case Fish.shellName | Fish.id => Fish.script(progName)
        case Zsh.shellName | Zsh.id   => Zsh.script(progName)
        case _ =>
          completeUnrecognizedFormat(format)
      }

    (completionsWorkingDirectory, args) match {
      case (Some(dir), Array("install", args0 @ _*)) =>
        completionsInstall(dir, args0)
      case (Some(dir), Array("uninstall", args0 @ _*)) =>
        completionsUninstall(dir, args0)
      case (_, Array(format, dest)) =>
        val script0 = script(format)
        writeCompletions(script0, dest)
      case (_, Array(format)) =>
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
        val items = complete(userArgs.toList.drop(1), index)
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
