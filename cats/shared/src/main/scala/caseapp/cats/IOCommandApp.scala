package caseapp.cats

import caseapp.core.commandparser.CommandParser
import caseapp.core.help.CommandsHelp
import cats.effect.{ExitCode, IO}

abstract class IOCommandApp[T](implicit
  commandParser: CommandParser[T],
  commandsMessages: CommandsHelp[T]
) extends IOCommandAppWithPreCommand[None.type , T] {

  override def beforeCommand(options:  None.type, remainingArgs:  Seq[String]): IO[Option[ExitCode]] = {
    if (remainingArgs.nonEmpty) IO {
      Console.err.println(s"Found extra arguments: ${remainingArgs.mkString(" ")}")
      Some(ExitCode(255))
    } else IO.none
  }
}
