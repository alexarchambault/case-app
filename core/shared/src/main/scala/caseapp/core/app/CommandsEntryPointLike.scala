package caseapp.core.app

import caseapp.core.commandparser.RuntimeCommandParser
import caseapp.core.help.{Help, HelpFormat, RuntimeCommandHelp, RuntimeCommandsHelp}

/** Help and command dispatch logic shared by [[CommandsEntryPoint]] and `IOCommandsEntryPoint` in
  * the cats-effect module
  */
trait CommandsEntryPointLike[C <: CommandLike] {

  /** Command to run when the arguments don't start with the name of a command
    *
    * `None` by default, in which case the help message of the entry point is printed in that case
    * (see `printUsage`). When set, all the arguments are passed to this command, and its options
    * are listed in the help message of the entry point.
    */
  def defaultCommand: Option[C] = None

  /** Commands of this entry point
    *
    * The command run is the one whose name (see [[CommandLike.names]]) matches the longest prefix
    * of the arguments. The arguments that follow the command name are passed to it.
    */
  def commands: Seq[C]

  /** Program name, used in help messages
    *
    * Commands are passed this program name followed by their own name, as program name for their
    * help messages (like `my-app my-command`).
    */
  def progName: String

  /** Description of the application, printed below the usage line of its help message
    *
    * Empty by default, in which case nothing is printed.
    */
  def description: String = ""

  /** Text printed at the end of the help message of the application
    *
    * Empty by default, in which case nothing is printed.
    */
  def summaryDesc: String = ""

  /** Help message of the application, listing its commands */
  def help: RuntimeCommandsHelp =
    RuntimeCommandsHelp(
      progName,
      Some(description).filter(_.nonEmpty),
      defaultCommand.map(_.finalHelp: Help[_]).getOrElse(Help[Unit]()),
      commands.map(cmd => RuntimeCommandHelp(cmd.names, cmd.finalHelp, cmd.group, cmd.hidden)),
      Some(summaryDesc).filter(_.nonEmpty)
    )

  /** Format of the help message of the application
    *
    * Defaults to [[caseapp.core.help.HelpFormat.default()* HelpFormat.default()]], which uses ANSI
    * colors. This doesn't change the format of the help messages of the commands, see
    * [[CaseApp.helpFormat]] for those.
    */
  def helpFormat: HelpFormat =
    HelpFormat.default()

  /** Finds the command to run for `args`
    *
    * Looks for the command whose name matches the longest prefix of `args`, falling back to
    * [[defaultCommand]] if no command matches.
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
