package caseapp.core.commandparser

import caseapp.core.parser.Parser
import dataclass.data

@deprecated("Use Command and CommandsEntryPoint instead", "2.1.0")
@data class MappedCommandParser[T, U](parser: CommandParser[T], f: T => U)
    extends CommandParser[U] {

  def commandMap: Map[Seq[String], Parser[U]] =
    parser
      .commandMap
      .iterator
      .map {
        case (c, p) =>
          c -> p.map(f)
      }
      .toMap

}
