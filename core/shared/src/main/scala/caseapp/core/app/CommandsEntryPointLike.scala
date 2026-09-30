package caseapp.core.app

import caseapp.core.commandparser.RuntimeCommandParser
import caseapp.core.help.{Help, HelpFormat, RuntimeCommandHelp, RuntimeCommandsHelp}

/** Help and command dispatch logic shared by [[CommandsEntryPoint]] and `IOCommandsEntryPoint` in
  * the cats-effect module
  */
trait CommandsEntryPointLike[C <: CommandLike] {

  def defaultCommand: Option[C] = None
  def commands: Seq[C]

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

  /** Finds the command to run for `args`
    *
    * @return
    *   the program name to use in that command's help messages, the command, and the arguments to
    *   pass to it, or `None` if no command matches and there's no default command
    */
  def commandFor(args: List[String]): Option[(String, C, List[String])] = {
    val map = RuntimeCommandParser.commandMap(commands)
    val res = defaultCommand match {
      case None =>
        RuntimeCommandParser.parse(map, args)
      case Some(defaultCommand0) =>
        Some(RuntimeCommandParser.parse(defaultCommand0, map, args))
    }
    res.map {
      case (commandName, command, commandArgs) =>
        ((progName +: commandName).mkString(" "), command, commandArgs)
    }
  }
}
