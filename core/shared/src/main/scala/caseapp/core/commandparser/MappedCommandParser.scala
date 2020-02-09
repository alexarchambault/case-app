package caseapp.core.commandparser

import caseapp.core.parser.Parser
import dataclass.data

@data class MappedCommandParser[T, U](parser: CommandParser[T], f: T => U) extends CommandParser[U] {

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
