package caseapp.core.commandparser

import caseapp.core.parser.Parser
import shapeless.CNil

@deprecated("Use Command and CommandsEntryPoint instead", "2.1.0")
case object NilCommandParser extends CommandParser[CNil] {

  def commandMap: Map[Seq[String], Parser[CNil]] =
    Map.empty

}
