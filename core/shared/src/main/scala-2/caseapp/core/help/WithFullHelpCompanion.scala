package caseapp.core.help

import caseapp.{ExtraName, Group, HelpMessage, Parser}
import caseapp.core.parser.{Argument, NilParser, StandardArgument}
import caseapp.core.{Arg, Error}
import caseapp.core.parser.RecursiveConsParser
import caseapp.core.util.Formatter

import shapeless.{HNil, :: => :*:}

abstract class WithFullHelpCompanion {

  implicit def parser[T, D](implicit
    underlying: Parser.Aux[T, D]
  ): Parser.Aux[
    WithFullHelp[T],
    (Option[Boolean] :*: Option[Boolean] :*: D :*: HNil) :*: Option[Boolean] :*: HNil
  ] = {

    val baseHelpArgument = StandardArgument[Boolean](
      Arg("helpFull")
        .withExtraNames(Seq(
          ExtraName("fullHelp"),
          ExtraName("-help-full"),
          ExtraName("-full-help")
        ))
        .withGroup(Some(Group("Help")))
        .withOrigin(Some("WithFullHelp"))
        .withHelpMessage(Some(
          HelpMessage("Print help message, including hidden options, and exit")
        ))
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
            case ("-help-full" | "-full-help") :: rem => Right(Some((Some(true), rem)))
            case _ => baseHelpArgument.step(args, index, d, nameFormatter)
          }
        def get(d: Option[Boolean], nameFormatter: Formatter[ExtraName]) =
          baseHelpArgument.get(d, nameFormatter)
      }

    val withHelpParser = WithHelp.parser[T, D](underlying)

    val p = RecursiveConsParser(withHelpParser, helpArgument :: NilParser)

    p.to[WithFullHelp[T]]
  }

}
