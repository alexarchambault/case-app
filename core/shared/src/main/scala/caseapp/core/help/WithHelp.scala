package caseapp.core.help

import caseapp.{ExtraName, Group, Help, HelpMessage, Parser, Recurse}
import caseapp.core.parser.{Argument, NilParser, StandardArgument}
import caseapp.core.{Arg, Error}
import caseapp.core.parser.{EitherParser, RecursiveConsParser}
import caseapp.core.util.Formatter

import shapeless.{HNil, :: => :*:}

/** Helper to add `--usage` and `--help` options to an existing type `T`.
  *
  * @param usage:
  *   whether usage was requested
  * @param help:
  *   whether help was requested
  * @param baseOrError:
  *   parsed `T` in case of success, or error message else
  * @tparam T:
  *   type to which usage and help options are added
  */
final case class WithHelp[T](
  @Group("Help")
  @HelpMessage("Print usage and exit")
  usage: Boolean = false,
  @Group("Help")
  @HelpMessage("Print help message and exit")
  @ExtraName("h")
  help: Boolean = false,
  @Recurse
  baseOrError: Either[Error, T]
) {
  def map[U](f: T => U): WithHelp[U] =
    copy(baseOrError = baseOrError.map(f))
}

object WithHelp {

  implicit def parser[T, D](implicit
    underlying: Parser.Aux[T, D]
  ): Parser.Aux[WithHelp[T], Option[Boolean] :*: Option[Boolean] :*: D :*: HNil] = {

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
          d: Option[Boolean],
          nameFormatter: Formatter[ExtraName]
        ): Either[(Error, List[String]), Option[(Option[Boolean], List[String])]] =
          args match {
            case "-help" :: rem => Right(Some((Some(true), rem)))
            case _              => baseHelpArgument.step(args, d, nameFormatter)
          }
        def get(d: Option[Boolean], nameFormatter: Formatter[ExtraName]) =
          baseHelpArgument.get(d, nameFormatter)
      }

    val either = EitherParser[T, D](underlying)

    val p = usageArgument ::
      helpArgument ::
      RecursiveConsParser(either, NilParser)

    p.to[WithHelp[T]]
  }

  implicit def help[T, D](implicit
    parser: Parser.Aux[T, D],
    underlying: Help[T]
  ): Help[WithHelp[T]] =
    Help.derive[WithHelp[T]]
}
