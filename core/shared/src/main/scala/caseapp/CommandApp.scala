package caseapp

import caseapp.core.{CommandsMessages, DefaultBaseCommand}

abstract class CommandApp[T](implicit
  commandParser: CommandParser[T],
  commandsMessages: CommandsMessages[T]
) extends CommandAppWithPreCommand[DefaultBaseCommand, T] {
  def beforeCommand(options: DefaultBaseCommand, remainingArgs: Seq[String]): Unit =
    if (remainingArgs.nonEmpty) {
      Console.err.println(s"Found extra arguments: ${remainingArgs.mkString(" ")}")
      sys.exit(255)
    }
}
