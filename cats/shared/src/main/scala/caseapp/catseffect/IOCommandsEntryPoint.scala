package caseapp.catseffect

import caseapp.core.commandparser.RuntimeCommandParser
import caseapp.core.help.{Help, HelpFormat, RuntimeCommandHelp, RuntimeCommandsHelp}
import cats.effect.{ExitCode, IO, IOApp}

abstract class IOCommandsEntryPoint extends IOApp {

  def defaultCommand: Option[IOCommand[_]] = None
  def commands: Seq[IOCommand[_]]

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

  private def commandMap: Map[List[String], IOCommand[_]] =
    commands.flatMap(cmd =>
      cmd.names.map(names => names -> cmd)
    ).toMap

  def printUsage(): IO[ExitCode] = {
    val usage = help.help(helpFormat, showHidden = false)
    IO(Console.println(usage)).as(ExitCode.Success)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val map = commandMap
    defaultCommand match {
      case None =>
        RuntimeCommandParser.parse(map, args) match {
          case None =>
            printUsage()
          case Some((commandName, command, commandArgs)) =>
            command.main(commandProgName(commandName), commandArgs.toArray)
        }
      case Some(defaultCommand0) =>
        RuntimeCommandParser.parse(defaultCommand0, map, args) match {
          case (commandName, command, commandArgs) =>
            command.main(commandProgName(commandName), commandArgs.toArray)
        }
    }
  }
}
