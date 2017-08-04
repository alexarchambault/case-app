package caseapp.core.commandparser

import shapeless.CNil

case object NilCommandParser extends CommandParser[CNil] {

  def get(command: String): None.type =
    None

}
