package caseapp.core.app

import caseapp.core.RemainingArgs
import caseapp.core.commandparser.CommandParser
import caseapp.core.help.CommandsHelp

/* The A suffix stands for anonymous */
abstract class CommandAppA[T](
  commandParser: CommandParser[T],
  commandsMessages: CommandsHelp[T]
) extends CommandApp[T]()(commandParser, commandsMessages) {

  def runA: RemainingArgs => T => Unit

  def run(options: T, remainingArgs: RemainingArgs): Unit =
    runA(remainingArgs)(options)

}
