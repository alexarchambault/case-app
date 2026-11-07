package caseapp.catseffect

import caseapp.core.app.CommandsEntryPointLike
import cats.effect.{ExitCode, IO, IOApp}

/** cats-effect version of [[caseapp.core.app.CommandsEntryPoint]], the entry point of an
  * application with several [[IOCommand]]s
  *
  * Extend it with an `object`, and define `progName` and `commands`. It's an `IOApp`, whose `run`
  * method looks for the command to run from the command-line arguments (see `commandFor`), and runs
  * it with the arguments that follow the command name. If no command matches, and there's no
  * `defaultCommand`, it prints the help message of the application, listing its commands.
  *
  * {{{
  * object MyApp extends IOCommandsEntryPoint {
  *   def progName = "my-app"
  *   def commands = Seq(Add, Remove) // Add and Remove extend IOCommand
  * }
  * }}}
  *
  * Unlike [[caseapp.core.app.CommandsEntryPoint]], it doesn't offer shell completions.
  */
abstract class IOCommandsEntryPoint extends IOApp with CommandsEntryPointLike[IOCommand[_]] {

  /** Called when no command matches the arguments, and there's no `defaultCommand`
    *
    * Prints the help message of the application, and returns `ExitCode.Success`.
    */
  def printUsage(): IO[ExitCode] = {
    val usage = help.help(helpFormat, showHidden = false)
    IO(Console.println(usage)).as(ExitCode.Success)
  }

  /** Entry point of the application, called by cats-effect
    *
    * Finds the command to run (see `commandFor`), and calls its [[IOCaseApp.main main]] method, or
    * calls [[printUsage]] if no command matches.
    */
  override def run(args: List[String]): IO[ExitCode] =
    commandFor(args) match {
      case None =>
        printUsage()
      case Some((commandProgName, command, commandArgs)) =>
        command.main(commandProgName, commandArgs.toArray)
    }
}
