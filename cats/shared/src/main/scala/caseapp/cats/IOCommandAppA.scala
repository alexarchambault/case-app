package caseapp.cats

import caseapp.core.commandparser.CommandParser
import caseapp.core.help.CommandsHelp
import caseapp.core.RemainingArgs
import cats.effect.{ExitCode, IO}

/* The A suffix stands for anonymous */
abstract class IOCommandAppA[T](implicit
  commandParser: CommandParser[T],
  commandsMessages: CommandsHelp[T]
) extends IOCommandApp[T]()(commandParser, commandsMessages) {

  def runA: RemainingArgs => T => IO[ExitCode]

  override def run(options: T, remainingArgs: RemainingArgs): IO[ExitCode] =
    runA(remainingArgs)(options)
}
