package caseapp.core.app

import caseapp.core.commandparser.RuntimeCommandParser
import caseapp.core.help.{Help, HelpFormat, RuntimeCommandsHelp}

abstract class CommandsEntryPoint {

  def defaultCommand: Option[Command[_]] = None
  def commands: Seq[Command[_]]

  def progName: String
  def description: String = ""

  def help: RuntimeCommandsHelp =
    RuntimeCommandsHelp(
      progName,
      Some(description).filter(_.nonEmpty),
      defaultCommand.map(_.messages.withHelp: Help[_]).getOrElse(Help[Unit]()),
      commands.map(cmd => (cmd.names, cmd.messages.withHelp))
    )

  def helpFormat: HelpFormat =
    HelpFormat.default()

  private def commandProgName(commandName: List[String]): String =
    (progName +: commandName).mkString(" ")

  def main(args: Array[String]): Unit =
    defaultCommand match {
      case None =>
        RuntimeCommandParser.parse(commands, args.toList) match {
          case None =>
            val usage = help.help(helpFormat)
            println(usage)
            sys.exit(0)
          case Some((commandName, command, commandArgs)) =>
            command.main(commandProgName(commandName), commandArgs.toArray)
        }
      case Some(defaultCommand0) =>
        val (commandName, command, commandArgs) =
          RuntimeCommandParser.parse(defaultCommand0, commands, args.toList)
        command.main(commandProgName(commandName), commandArgs.toArray)
    }
}
