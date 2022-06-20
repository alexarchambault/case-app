package caseapp.core

import caseapp.core.help.Help
import caseapp.core.help.HelpFormat
import caseapp.core.parser.StandardArgument
import caseapp.core.util.fansi
import caseapp.core.parser.EitherParser
import caseapp.core.parser.Parser
import caseapp.core.parser.IgnoreUnrecognizedParser
import caseapp.core.parser.MappedParser
import caseapp.core.parser.OptionParser
import caseapp.core.parser.ParserWithNameFormatter
import caseapp.core.parser.StopAtFirstUnrecognizedParser

object Scala3Helpers {

  implicit class ArgsWithOps(private val arg: Arg) extends AnyVal {
    def withOrigin(origin: Option[String]): Arg =
      arg.copy(origin = origin)
  }

  implicit class SeveralErrorsWithOps(private val error: Error.SeveralErrors) extends AnyVal {
    def withTail(tail: Seq[Error.SimpleError]): Error.SeveralErrors =
      error.copy(tail = tail)
  }

  implicit class HelpWithOps[T](private val help: Help[T]) extends AnyVal {
    def withArgs(args: Seq[Arg]): Help[T] =
      help.copy(args = args)
    def withProgName(progName: String): Help[T] =
      help.copy(progName = progName)
  }

  implicit class HelpFormatWithOps(private val helpFormat: HelpFormat) extends AnyVal {
    def withProgName(progName: fansi.Attrs): HelpFormat =
      helpFormat.copy(progName = progName)
    def withCommandName(commandName: fansi.Attrs): HelpFormat =
      helpFormat.copy(commandName = commandName)
    def withOption(option: fansi.Attrs): HelpFormat =
      helpFormat.copy(option = option)
    def withHidden(hidden: fansi.Attrs): HelpFormat =
      helpFormat.copy(hidden = hidden)
  }

  implicit class StandardArgumentWithOps[H](private val standardArg: StandardArgument[H])
      extends AnyVal {
    def withArg(arg: Arg): StandardArgument[H] =
      standardArg.copy(arg = arg)
  }

  implicit class EitherParserWithOps[T, D0](private val parser: EitherParser[T, D0])
      extends AnyVal {
    def withUnderlying(underlying: Parser.Aux[T, D0]): EitherParser[T, D0] =
      parser.copy(underlying = underlying)
  }

  implicit class IgnoreUnrecognizedParserWithOps[T, D0](
    private val parser: IgnoreUnrecognizedParser[T, D0]
  ) extends AnyVal {
    def withUnderlying(underlying: Parser.Aux[T, D0]): IgnoreUnrecognizedParser[T, D0] =
      parser.copy(underlying = underlying)
  }

  implicit class MappedParserWithOps[T, D0, U](private val parser: MappedParser[T, D0, U])
      extends AnyVal {
    def withUnderlying(underlying: Parser.Aux[T, D0]): MappedParser[T, D0, U] =
      parser.copy(underlying = underlying)
  }

  implicit class OptionParserWithOps[T, D0](private val parser: OptionParser[T, D0])
      extends AnyVal {
    def withUnderlying(underlying: Parser.Aux[T, D0]): OptionParser[T, D0] =
      parser.copy(underlying = underlying)
  }

  implicit class ParserWithNameFormatterWithOps[T, D0](private val parser: ParserWithNameFormatter[
    T,
    D0
  ]) extends AnyVal {
    def withUnderlying(underlying: Parser.Aux[T, D0]): ParserWithNameFormatter[T, D0] =
      parser.copy(underlying = underlying)
  }

  implicit class StopAtFirstUnrecognizedParserWithOps[T, D0](
    private val parser: StopAtFirstUnrecognizedParser[T, D0]
  ) extends AnyVal {
    def withUnderlying(underlying: Parser.Aux[T, D0]): StopAtFirstUnrecognizedParser[T, D0] =
      parser.copy(underlying = underlying)
  }

}
