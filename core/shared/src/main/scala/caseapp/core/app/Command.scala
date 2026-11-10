package caseapp.core.app

import caseapp.core.parser.Parser
import caseapp.core.help.Help

/** A command of an application with several commands, whose options are a `T`
  *
  * Like [[CaseApp]], implement [[CaseApp.run run]] in an `object` extending it. Then list the
  * commands in the [[CommandsEntryPoint.commands commands]] of a [[CommandsEntryPoint]], which
  * dispatches the command-line arguments to the right command.
  *
  * {{{
  * case class AddOptions(force: Boolean = false)
  *
  * object Add extends Command[AddOptions] {
  *   override def names = List(List("add"), List("a"))
  *   def run(options: AddOptions, remainingArgs: RemainingArgs): Unit = ???
  * }
  *
  * object MyApp extends CommandsEntryPoint {
  *   def progName = "my-app"
  *   def commands = Seq(Add)
  * }
  * }}}
  *
  * All the members of [[CaseApp]] can be overridden, to customize help, parsing, etc.
  *
  * See `caseapp.catseffect.IOCommand` in the case-app-cats module for a cats-effect version of this
  * class.
  *
  * @tparam T
  *   type of the options of the command
  */
abstract class Command[T](implicit parser: Parser[T], help: Help[T])
    extends CaseApp()(parser, help) with CommandLike {

  /** Names of this command
    *
    * Each name is a list of words: `List("foo", "bar")` corresponds to the `foo bar` command (run
    * with `my-app foo bar …`). Several names can be given, to define aliases.
    *
    * Defaults to a single one-word name, [[CaseApp.name name]], which is itself derived from the
    * name of `T` (`AddOptions` gives `add`) or its `ProgName` annotation.
    */
  def names: List[List[String]] =
    List(List(name))

  /** Group of this command, in the command list of the help message of the entry point
    *
    * Commands of the same group are listed together, under a `<group> commands:` heading. Commands
    * with an empty group (the default) are listed under `Commands:` (or `Other commands:` if some
    * commands have a group). The order of the groups can be changed with
    * [[caseapp.core.help.HelpFormat.sortedCommandGroups]] or
    * [[caseapp.core.help.HelpFormat.sortCommandGroups]].
    */
  def group: String = ""

  /** Whether this command is hidden from the command list of the help message of the entry point
    *
    * `false` by default. Hidden commands can still be run.
    */
  def hidden: Boolean = false
}
