package caseapp.catseffect

import caseapp.core.app.CommandLike
import caseapp.core.help.Help
import caseapp.core.parser.Parser

/** cats-effect version of [[caseapp.core.app.Command]], a command of an [[IOCommandsEntryPoint]],
  * whose options are a `T`
  *
  * Like [[IOCaseApp]], implement its `run` method, returning an `IO[ExitCode]`, in an `object`
  * extending it. Then list the commands in the `commands` of an [[IOCommandsEntryPoint]].
  *
  * @tparam T
  *   type of the options of the command
  */
abstract class IOCommand[T](implicit parser: Parser[T], help: Help[T])
    extends IOCaseApp()(parser, help) with CommandLike {

  /** Names of this command, see [[caseapp.core.app.Command.names]]
    *
    * Defaults to a single one-word name, [[IOCaseApp.name name]].
    */
  def names: List[List[String]] =
    List(List(name))

  /** Group of this command, in the command list of the help message of the entry point, see
    * [[caseapp.core.app.Command.group]]
    */
  def group: String = ""

  /** Whether this command is hidden from the command list of the help message of the entry point
    *
    * `false` by default. Hidden commands can still be run.
    */
  def hidden: Boolean = false
}
