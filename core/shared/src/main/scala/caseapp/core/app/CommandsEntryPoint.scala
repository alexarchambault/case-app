package caseapp.core.app

import caseapp.core.commandparser.RuntimeCommandParser
import caseapp.core.complete.{Bash, CompletionItem, Zsh}
import caseapp.core.help.{Help, HelpFormat, RuntimeCommandsHelp}
import caseapp.core.help.RuntimeCommandHelp

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

  def completionsMain(args: Array[String]): Unit = {

    def script(format: String): String =
      format match {
        case Bash.shellName | Bash.id => Bash.script(progName)
        case Zsh.shellName | Zsh.id   => Zsh.script(progName)
        case _ =>
          System.err.println(s"Unrecognized completion format '$format'")
          PlatformUtil.exit(1)
      }
    args match {
      case Array(format, dest) =>
        val script0 = script(format)
        writeCompletions(script0, dest)
      case Array(format) =>
        val script0 = script(format)
        println(script0)
      case _ =>
        System.err.println(s"Usage: $progName $completionsCommandName format [dest]")
        PlatformUtil.exit(1)
    }
  }

  def complete(args: Seq[String], index: Int): List[CompletionItem] =
    defaultCommand match {
      case None =>
        RuntimeCommandParser.complete(commands, args.toList, index)
      case Some(defaultCommand0) =>
        RuntimeCommandParser.complete(defaultCommand0, commands, args.toList, index)
    }

  def completeMain(args: Array[String]): Unit =
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
            println(Bash.print(items))
          case Zsh.id =>
            println(Zsh.print(items))
          case _ =>
            System.err.println(s"Unrecognized completion format '$format'")
            PlatformUtil.exit(1)
        }
      case _ =>
        System.err.println(s"Usage: $progName $completeCommandName format index ...args...")
        PlatformUtil.exit(1)
    }

  def main(args: Array[String]): Unit = {
    val actualArgs = PlatformUtil.arguments(args)
    if (enableCompleteCommand && actualArgs.startsWith(completeCommandName.toArray[String]))
      completeMain(actualArgs.drop(completeCommandName.length))
    else if (
      enableCompletionsCommand && actualArgs.startsWith(completionsCommandName.toArray[String])
    )
      completionsMain(actualArgs.drop(completionsCommandName.length))
    else
      defaultCommand match {
        case None =>
          RuntimeCommandParser.parse(commands, actualArgs.toList) match {
            case None =>
              val usage = help.help(helpFormat, showHidden = false)
              println(usage)
              PlatformUtil.exit(0)
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
