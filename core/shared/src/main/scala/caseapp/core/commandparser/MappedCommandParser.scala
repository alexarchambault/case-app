package caseapp.core.commandparser

import caseapp.core.parser.Parser

final case class MappedCommandParser[T, U](parser: CommandParser[T], f: T => U) extends CommandParser[U] {

  def get(command: String): Option[Parser[U]] =
    parser
      .get(command)
      .map(_.map(f))

}
