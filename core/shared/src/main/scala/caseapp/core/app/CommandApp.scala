package caseapp.core.app

import caseapp.core.commandparser.CommandParser
import caseapp.core.help.CommandsHelp

abstract class CommandApp[T](implicit
  commandParser: CommandParser[T],
  commandsMessages: CommandsHelp[T]
) extends CommandAppWithPreCommand[None.type, T] {
  def beforeCommand(options: None.type, remainingArgs: Seq[String]): Unit =
    if (remainingArgs.nonEmpty) {
      Console.err.println(s"Found extra arguments: ${remainingArgs.mkString(" ")}")
      sys.exit(255)
    }
}
