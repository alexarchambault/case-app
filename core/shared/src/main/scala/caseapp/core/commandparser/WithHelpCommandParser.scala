package caseapp.core.commandparser

import caseapp.core.parser.Parser
import caseapp.core.help.WithHelp
import dataclass.data

@deprecated("Use Command and CommandsEntryPoint instead", "2.1.0")
@data class WithHelpCommandParser[T](parser: CommandParser[T]) extends CommandParser[WithHelp[T]] {

  def commandMap: Map[Seq[String], Parser[WithHelp[T]]] =
    parser
      .commandMap
      .iterator
      .map {
        case (c, p) =>
          c -> p.withHelp
      }
      .toMap

}
