package caseapp.core.commandparser

import caseapp.core.parser.Parser

final case class MappedCommandParser[T, U](parser: CommandParser[T], f: T => U) extends CommandParser[U] {

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
