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

/** Setters for Scala 3, where data-class doesn't generate any
  *
  * Only kept for compatibility purposes. Use `copy` instead, which is available on both Scala 2 and
  * Scala 3.
  */
@deprecated("Use copy instead of the methods it adds", "2.1.2")
object Scala3Helpers {

  implicit class ArgsWithOps(private val arg: Arg) extends AnyVal {
    @deprecated("Use copy instead", "2.1.2")
    def withOrigin(origin: Option[String]): Arg =
      arg.copy(origin = origin)
    @deprecated("Use copy instead", "2.1.2")
    def withIsFlag(isFlag: Boolean): Arg =
      arg.copy(isFlag = isFlag)
    @deprecated("Use copy instead", "2.1.2")
    def withGroup(group: Option[Group]): Arg =
      arg.copy(group = group)
    @deprecated("Use copy instead", "2.1.2")
    def withHelpMessage(helpMessage: Option[HelpMessage]): Arg =
      arg.copy(helpMessage = helpMessage)
    @deprecated("Use copy instead", "2.1.2")
    def withExtraNames(extraNames: Seq[Name]): Arg =
      arg.copy(extraNames = extraNames)
  }

  implicit class SeveralErrorsWithOps(private val error: Error.SeveralErrors) extends AnyVal {
    @deprecated("Use copy instead", "2.1.2")
    def withTail(tail: Seq[Error.SimpleError]): Error.SeveralErrors =
      error.copy(tail = tail)
  }

  implicit class HelpWithOps[T](private val help: Help[T]) extends AnyVal {
    @deprecated("Use copy instead", "2.1.2")
    def withArgs(args: Seq[Arg]): Help[T] =
      help.copy(args = args)
    @deprecated("Use copy instead", "2.1.2")
    def withProgName(progName: String): Help[T] =
      help.copy(progName = progName)
  }

  implicit class HelpFormatWithOps(private val helpFormat: HelpFormat) extends AnyVal {
    @deprecated("Use copy instead", "2.1.2")
    def withProgName(progName: fansi.Attrs): HelpFormat =
      helpFormat.copy(progName = progName)
    @deprecated("Use copy instead", "2.1.2")
    def withCommandName(commandName: fansi.Attrs): HelpFormat =
      helpFormat.copy(commandName = commandName)
    @deprecated("Use copy instead", "2.1.2")
    def withOption(option: fansi.Attrs): HelpFormat =
      helpFormat.copy(option = option)
    @deprecated("Use copy instead", "2.1.2")
    def withHidden(hidden: fansi.Attrs): HelpFormat =
      helpFormat.copy(hidden = hidden)
    @deprecated("Use copy instead", "2.1.2")
    def withSortGroups(sortGroups: Option[Seq[String] => Seq[String]]): HelpFormat =
      helpFormat.copy(sortGroups = sortGroups)
    @deprecated("Use copy instead", "2.1.2")
    def withSortedGroups(sortedGroups: Option[Seq[String]]): HelpFormat =
      helpFormat.copy(sortedGroups = sortedGroups)
    @deprecated("Use copy instead", "2.1.2")
    def withHiddenGroups(hiddenGroups: Option[Seq[String]]): HelpFormat =
      helpFormat.copy(hiddenGroups = hiddenGroups)

    @deprecated("Use copy instead", "2.1.2")
    def withFilterArgs(filterArgs: Option[Arg => Boolean]): HelpFormat =
      helpFormat.copy(filterArgs = filterArgs)

    @deprecated("Use copy instead", "2.1.2")
    def withFilterArgsWhenShowHidden(filterArgs: Option[Arg => Boolean]): HelpFormat =
      helpFormat.copy(filterArgsWhenShowHidden = filterArgs)

    @deprecated("Use copy instead", "2.1.2")
    def withHiddenGroupsWhenShowHidden(hiddenGroups: Option[Seq[String]]): HelpFormat =
      helpFormat.copy(hiddenGroupsWhenShowHidden = hiddenGroups)

    @deprecated("Use copy instead", "2.1.2")
    def withNamesLimit(newNamesLimit: Option[Int]): HelpFormat =
      helpFormat.copy(namesLimit = newNamesLimit)

    @deprecated("Use copy instead", "2.1.2")
    def withSortedCommandGroups(newSortedCommandGroups: Option[Seq[String]]): HelpFormat =
      helpFormat.copy(sortedCommandGroups = newSortedCommandGroups)
  }

  implicit class OptionParserWithOps[T](private val parser: OptionParser[T]) {
    @deprecated("Use copy instead", "2.1.2")
    def withUnderlying(underlying: Parser[T]): OptionParser[T] =
      parser.copy(underlying = underlying)
  }

  implicit class ConsParserWithOps[H, T <: Tuple](private val parser: ConsParser[H, T])
      extends AnyVal {
    @deprecated("Use copy instead", "2.1.2")
    def withArgument(argument: Argument[H]): ConsParser[H, T] =
      parser.copy(argument = argument)
    @deprecated("Use copy instead", "2.1.2")
    def withTail(tail: Parser[T]): ConsParser[H, T] =
      parser.copy(tail = tail)
  }

  implicit class RecursiveConsParserWithOps[H, T <: Tuple](
    private val parser: RecursiveConsParser[H, T]
  ) extends AnyVal {
    @deprecated("Use copy instead", "2.1.2")
    def withHeadParser(headParser: Parser[H]): RecursiveConsParser[H, T] =
      parser.copy(headParser = headParser)
    @deprecated("Use copy instead", "2.1.2")
    def withTailParser(tailParser: Parser[T]): RecursiveConsParser[H, T] =
      parser.copy(tailParser = tailParser)
  }

  implicit class StandardArgumentWithOps[H](private val arg: StandardArgument[H]) extends AnyVal {
    @deprecated("Use copy instead", "2.1.2")
    def withDefault(default: () => Option[H]): StandardArgument[H] =
      arg.copy(default = default)
  }

}
