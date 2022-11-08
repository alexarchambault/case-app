package caseapp.core.help

import caseapp.core.parser.Parser
import caseapp.{ExtraName, HelpMessage}

abstract class WithFullHelpCompanion {

  implicit def parser[T, D](implicit underlying: Parser.Aux[T, D]): Parser[WithFullHelp[T]] =
    Parser.nil
      .addAll[WithHelp[T]].apply
      .add[Boolean](
        "helpFull",
        default = Some(false),
        extraNames = Seq(ExtraName("fullHelp")),
        helpMessage = Some(HelpMessage("Print help message, including hidden options, and exit"))
      )
      .as[WithFullHelp[T]]

}
