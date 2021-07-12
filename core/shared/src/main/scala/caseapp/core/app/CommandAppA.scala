package caseapp.core.app

import caseapp.core.RemainingArgs
import caseapp.core.commandparser.CommandParser
import caseapp.core.help.CommandsHelp

/* The A suffix stands for anonymous */
@deprecated("Use Command and CommandsEntryPoint instead", "2.1.0")
abstract class CommandAppA[T](
  commandParser: CommandParser[T],
  commandsMessages: CommandsHelp[T]
) extends CommandApp[T]()(commandParser, commandsMessages) {

  def runA: RemainingArgs => T => Unit

  def run(options: T, remainingArgs: RemainingArgs): Unit =
    runA(remainingArgs)(options)

}
