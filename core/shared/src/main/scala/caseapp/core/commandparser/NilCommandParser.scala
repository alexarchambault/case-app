package caseapp.core.commandparser

import caseapp.core.parser.Parser
import shapeless.CNil

case object NilCommandParser extends CommandParser[CNil] {

  def commandMap: Map[Seq[String], Parser[CNil]] =
    Map.empty

}
