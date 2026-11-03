package caseapp.core.help

import caseapp.{ExtraName, Group, HelpMessage, Parser, Recurse}
import caseapp.core.parser.{Argument, NilParser, StandardArgument}
import caseapp.core.{Arg, Error}
import caseapp.core.parser.{EitherParser, RecursiveConsParser}
import caseapp.core.util.Formatter

import shapeless.{HNil, :: => :*:}

abstract class WithHelpCompanion {

  implicit def parser[T, D](implicit
    underlying: Parser.Aux[T, D]
  ): Parser.Aux[WithHelp[T], Option[Boolean] :*: Option[Boolean] :*: D :*: HNil] = {

    val usageArgument = StandardArgument[Boolean](
      Arg("usage")
        .copy(
          group = Some(Group("Help")),
          origin = Some("WithHelp"),
          helpMessage = Some(HelpMessage("Print usage and exit")),
          isFlag = true
        )
    ).copy(default = () => Some(false))

    val baseHelpArgument = StandardArgument[Boolean](
      Arg("help")
        .copy(
          extraNames = Seq(ExtraName("h"), ExtraName("-help")),
          group = Some(Group("Help")),
          origin = Some("WithHelp"),
          helpMessage = Some(HelpMessage("Print help message and exit")),
          isFlag = true
        )
    ).copy(default = () => Some(false))

    // accept "-help" too (single dash)
    val helpArgument: Argument[Boolean] =
      new Argument[Boolean] {
        def arg                               = baseHelpArgument.arg
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

    val either = EitherParser[T, D](underlying)

    val p = usageArgument ::
      helpArgument ::
      RecursiveConsParser(either, NilParser, Recurse())

    p.to[WithHelp[T]]
  }

}
