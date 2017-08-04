package caseapp.core.commandparser

import caseapp.core.parser.Parser
import caseapp.core.help.WithHelp

final case class WithHelpCommandParser[T](parser: CommandParser[T]) extends CommandParser[WithHelp[T]] {

  def get(command: String): Option[Parser[WithHelp[T]]] =
    parser
      .get(command)
      .map(_.withHelp)

}
