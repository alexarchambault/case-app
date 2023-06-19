package caseapp.core

import caseapp.{Group, HelpMessage, Name}
import caseapp.core.help.Help
import caseapp.core.help.HelpFormat
import caseapp.core.parser.StandardArgument
import caseapp.core.util.fansi
import caseapp.core.parser.Argument
import caseapp.core.parser.ConsParser
import caseapp.core.parser.EitherParser
import caseapp.core.parser.Parser
import caseapp.core.parser.IgnoreUnrecognizedParser
import caseapp.core.parser.MappedParser
import caseapp.core.parser.OptionParser
import caseapp.core.parser.ParserWithNameFormatter
import caseapp.core.parser.StopAtFirstUnrecognizedParser
import caseapp.core.parser.RecursiveConsParser

object Scala3Helpers {

  implicit class ArgsWithOps(private val arg: Arg) extends AnyVal {
    def withOrigin(origin: Option[String]): Arg =
      arg.copy(origin = origin)
    def withIsFlag(isFlag: Boolean): Arg =
      arg.copy(isFlag = isFlag)
    def withGroup(group: Option[Group]): Arg =
      arg.copy(group = group)
    def withHelpMessage(helpMessage: Option[HelpMessage]): Arg =
      arg.copy(helpMessage = helpMessage)
    def withExtraNames(extraNames: Seq[Name]): Arg =
      arg.copy(extraNames = extraNames)
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
    def withSortGroups(sortGroups: Option[Seq[String] => Seq[String]]): HelpFormat =
      helpFormat.copy(sortGroups = sortGroups)
    def withSortedGroups(sortedGroups: Option[Seq[String]]): HelpFormat =
      helpFormat.copy(sortedGroups = sortedGroups)
    def withHiddenGroups(hiddenGroups: Option[Seq[String]]): HelpFormat =
      helpFormat.copy(hiddenGroups = hiddenGroups)

    def withFilterArgs(filterArgs: Option[Arg => Boolean]): HelpFormat =
      helpFormat.copy(filterArgs = filterArgs)

    def withFilterArgsWhenShowHidden(filterArgs: Option[Arg => Boolean]): HelpFormat =
      helpFormat.copy(filterArgsWhenShowHidden = filterArgs)

    def withHiddenGroupsWhenShowHidden(hiddenGroups: Option[Seq[String]]): HelpFormat =
      helpFormat.copy(hiddenGroupsWhenShowHidden = hiddenGroups)

    def withNamesLimit(newNamesLimit: Option[Int]): HelpFormat =
      helpFormat.copy(namesLimit = newNamesLimit)
  }

  implicit class OptionParserWithOps[T](private val parser: OptionParser[T]) {
    def withUnderlying(underlying: Parser[T]): OptionParser[T] =
      parser.copy(underlying = underlying)
  }

  implicit class ConsParserWithOps[H, T <: Tuple](private val parser: ConsParser[H, T])
      extends AnyVal {
    def withArgument(argument: Argument[H]): ConsParser[H, T] =
      parser.copy(argument = argument)
    def withTail(tail: Parser[T]): ConsParser[H, T] =
      parser.copy(tail = tail)
  }

  implicit class RecursiveConsParserWithOps[H, T <: Tuple](
    private val parser: RecursiveConsParser[H, T]
  ) extends AnyVal {
    def withHeadParser(headParser: Parser[H]): RecursiveConsParser[H, T] =
      parser.copy(headParser = headParser)
    def withTailParser(tailParser: Parser[T]): RecursiveConsParser[H, T] =
      parser.copy(tailParser = tailParser)
  }

  implicit class StandardArgumentWithOps[H](private val arg: StandardArgument[H]) extends AnyVal {
    def withDefault(default: () => Option[H]): StandardArgument[H] =
      arg.copy(default = default)
  }

}
