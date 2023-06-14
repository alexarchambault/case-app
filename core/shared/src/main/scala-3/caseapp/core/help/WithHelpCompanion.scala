package caseapp.core.help

import caseapp.{ExtraName, Group, Help, HelpMessage, Parser}
import caseapp.core.Scala3Helpers.*
import caseapp.core.parser.{Argument, NilParser, StandardArgument}
import caseapp.core.{Arg, Error}
import caseapp.core.parser.{EitherParser, RecursiveConsParser}
import caseapp.core.util.Formatter
import caseapp.Recurse

abstract class WithHelpCompanion {

  implicit def parser[T](implicit
    underlying: Parser[T]
  ): Parser[WithHelp[T]] = {

    val usageArgument = StandardArgument[Boolean](
      Arg("usage")
        .withGroup(Some(Group("Help")))
        .withOrigin(Some("WithHelp"))
        .withHelpMessage(Some(HelpMessage("Print usage and exit")))
        .withIsFlag(true)
    ).withDefault(() => Some(false))

    val baseHelpArgument = StandardArgument[Boolean](
      Arg("help")
        .withExtraNames(Seq(ExtraName("h"), ExtraName("-help")))
        .withGroup(Some(Group("Help")))
        .withOrigin(Some("WithHelp"))
        .withHelpMessage(Some(HelpMessage("Print help message and exit")))
        .withIsFlag(true)
    ).withDefault(() => Some(false))

    // accept "-help" too (single dash)
    val helpArgument: Argument[Boolean] =
      new Argument[Boolean] {
        def arg = baseHelpArgument.arg
        def withDefaultOrigin(origin: String) =
          this
        def init = baseHelpArgument.init
        def step(
          args: List[String],
          index: Int,
          d: Option[Boolean],
          nameFormatter: Formatter[ExtraName]
        ): Either[(Error, List[String]), Option[(Option[Boolean], List[String])]] =
          args match {
            case "-help" :: rem => Right(Some((Some(true), rem)))
            case _              => baseHelpArgument.step(args, index, d, nameFormatter)
          }
        def get(d: Option[Boolean], nameFormatter: Formatter[ExtraName]) =
          baseHelpArgument.get(d, nameFormatter)
      }

    val either = EitherParser[T](underlying)

    val p = usageArgument ::
      helpArgument ::
      RecursiveConsParser(either, NilParser, Recurse())

    p.to[WithHelp[T]]
  }

  implicit def help[T, D](implicit
    parser: Parser[T],
    underlying: Help[T]
  ): Help[WithHelp[T]] =
    Help.derive[WithHelp[T]]
}
