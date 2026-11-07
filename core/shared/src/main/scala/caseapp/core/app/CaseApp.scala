package caseapp.core.app

import caseapp.Name
import caseapp.core.{Error, RemainingArgs}
import caseapp.core.complete.{Completer, CompletionItem, HelpCompleter}
import caseapp.core.help.{Help, HelpFormat, WithFullHelp, WithHelp}
import caseapp.core.parser.Parser
import caseapp.core.util.Formatter

/** Base class for single-command applications, whose options are a `T`
  *
  * Extend it with an `object`, and implement [[run]]. Its `main` method parses the command-line
  * arguments as a `T`, handles help options (`--help`, `--usage`…), prints errors, and calls
  * [[run]] with the parsed options and the remaining (non-option) arguments.
  *
  * {{{
  * case class Options(
  *   @HelpMessage("Name of the person to greet")
  *   name: String = "World"
  * )
  *
  * object Hello extends CaseApp[Options] {
  *   def run(options: Options, remainingArgs: RemainingArgs): Unit =
  *     println("Hello " + options.name)
  * }
  * }}}
  *
  * Its behaviour can be customized by overriding some of its members, like [[hasFullHelp]],
  * [[stopAtFirstUnrecognized]], [[ignoreUnrecognized]], [[nameFormatter]], [[helpFormat]],
  * [[expandArgs]], or [[exit]] and `printLine` (which all printing and exiting go through).
  *
  * See [[Command]] and [[CommandsEntryPoint]] for applications with several commands, and
  * `caseapp.catseffect.IOCaseApp` in the case-app-cats module for a cats-effect version of this
  * class.
  *
  * @param parser0
  *   parser for `T`, usually derived automatically
  * @param messages
  *   help messages for `T`, usually derived automatically
  * @tparam T
  *   type of the options of the application
  */
abstract class CaseApp[T](implicit val parser0: Parser[T], val messages: Help[T]) {

  /** Program name, used in help messages
    *
    * Defaults to the program name of [[help]], itself derived from the `ProgName` annotation of
    * `T`, or from its class name (see [[caseapp.core.help.Help]]).
    */
  def name: String =
    help.progName

  /** Whether to accept the `--help` / `-h` and `--usage` options
    *
    * `true` by default. When these options are passed, [[helpAsked]] or [[usageAsked]] are called
    * instead of [[run]]. Ignored if [[hasFullHelp]] is `true`.
    */
  def hasHelp: Boolean = true

  /** Whether to accept the `--help-full` / `--full-help` option, along with the options enabled by
    * [[hasHelp]]
    *
    * `false` by default. When this option is passed, [[fullHelpAsked]] is called instead of
    * [[run]]: it prints a help message including the arguments marked with the `Hidden` annotation,
    * and that uses the `detailedMessage` of the `HelpMessage` annotation of `T`, if any.
    */
  def hasFullHelp: Boolean = false

  /** Help messages for `T`, before the help options are added to it
    *
    * See [[finalHelp]] for the help that is actually printed.
    */
  def help: Help[T] = messages

  /** Parser for `T`, with [[nameFormatter]], [[ignoreUnrecognized]], and
    * [[stopAtFirstUnrecognized]] applied to it
    *
    * Help options are added to it in `main`, depending on [[hasHelp]] and [[hasFullHelp]].
    */
  def parser: Parser[T] = {
    val p = parser0.nameFormatter(nameFormatter)
    if (ignoreUnrecognized)
      p.ignoreUnrecognized
    else if (stopAtFirstUnrecognized)
      p.stopAtFirstUnrecognized
    else
      p
  }

  /** Completer used by [[complete]]
    *
    * By default, a [[caseapp.core.complete.HelpCompleter]], that completes option names using
    * [[messages]], and doesn't complete option values or arguments. Override it to provide custom
    * completions, for example with
    * [[caseapp.core.complete.Completer.CompleterOps.completeOptionValue completeOptionValue]].
    */
  def completer: Completer[T] =
    new HelpCompleter[T](messages)

  /** Computes shell completions
    *
    * Called by [[CommandsEntryPoint]] when completing the arguments of this command.
    *
    * @param args
    *   arguments of the application (without the program or command name)
    * @param index
    *   index in `args` of the argument to complete (it can be equal to `args.length`, to complete a
    *   new argument)
    * @return
    *   completion candidates
    */
  def complete(args: Seq[String], index: Int): List[CompletionItem] =
    if (hasFullHelp)
      parser.withFullHelp.complete(
        args,
        index,
        completer.withFullHelp,
        stopAtFirstUnrecognized,
        ignoreUnrecognized
      )
    else if (hasHelp)
      parser.withHelp.complete(
        args,
        index,
        completer.withHelp,
        stopAtFirstUnrecognized,
        ignoreUnrecognized
      )
    else
      parser.complete(
        args,
        index,
        completer,
        stopAtFirstUnrecognized,
        ignoreUnrecognized
      )

  /** Runs the application, once the arguments have been successfully parsed
    *
    * @param options
    *   parsed options
    * @param remainingArgs
    *   arguments that weren't parsed as options, see [[caseapp.core.RemainingArgs]]
    */
  def run(options: T, remainingArgs: RemainingArgs): Unit

  /** Exits the application, with exit code `code`
    *
    * Called after printing help messages (with exit code 0) and errors (with exit code 1). Calls
    * `sys.exit` on the JVM and Scala Native, and `process.exit` on Scala.js. Can be overridden, to
    * throw an exception instead in tests for example.
    */
  def exit(code: Int): Nothing =
    PlatformUtil.exit(code)

  /** Prints a line, on stdout or on stderr
    *
    * Help and error messages are printed with this method. Can be overridden, to capture output in
    * tests for example.
    */
  def printLine(line: String, toStderr: Boolean): Unit =
    if (toStderr)
      System.err.println(line)
    else
      println(line)

  /** Prints a line on stdout */
  final def printLine(line: String): Unit =
    printLine(line, toStderr = false)

  /** Called when the arguments can't be parsed
    *
    * By default, prints the error message on stderr, and exits with exit code 1.
    */
  def error(message: Error): Nothing = {
    printLine(message.message, toStderr = true)
    exit(1)
  }

  /** Help messages actually printed: [[help]], with the help options enabled by [[hasHelp]] or
    * [[hasFullHelp]] added to it, and with [[name]] as program name
    */
  lazy val finalHelp: Help[_] = {
    val h =
      if (hasFullHelp) messages.withFullHelp
      else if (hasHelp) messages.withHelp
      else messages
    if (name == h.progName) h
    else h.copy(progName = name)
  }

  /** Called when `--help-full` is passed, if [[hasFullHelp]] is `true`
    *
    * Prints the full help message (including hidden options), and exits with exit code 0.
    *
    * @param progName
    *   program name to use in the help message, or empty to use the one of [[finalHelp]]. When run
    *   from a [[CommandsEntryPoint]], this is the program name followed by the command name.
    */
  def fullHelpAsked(progName: String): Nothing = {
    val help = if (progName.isEmpty) finalHelp else finalHelp.copy(progName = progName)
    printLine(help.help(helpFormat, showHidden = true))
    exit(0)
  }

  /** Called when `--help` or `-h` is passed, if [[hasHelp]] or [[hasFullHelp]] is `true`
    *
    * Prints the help message, and exits with exit code 0.
    *
    * @param progName
    *   program name to use in the help message, or empty to use the one of [[finalHelp]]. When run
    *   from a [[CommandsEntryPoint]], this is the program name followed by the command name.
    * @param maybeOptions
    *   the result of parsing the other arguments (unused by default)
    */
  def helpAsked(progName: String, maybeOptions: Either[Error, T]): Nothing = {
    val help = if (progName.isEmpty) finalHelp else finalHelp.copy(progName = progName)
    printLine(help.help(helpFormat, showHidden = false))
    exit(0)
  }

  /** Called when `--usage` is passed, if [[hasHelp]] or [[hasFullHelp]] is `true`
    *
    * Prints the one-line usage message, and exits with exit code 0.
    *
    * @param progName
    *   program name to use in the usage message, or empty to use the one of [[finalHelp]]. When run
    *   from a [[CommandsEntryPoint]], this is the program name followed by the command name.
    * @param maybeOptions
    *   the result of parsing the other arguments (unused by default)
    */
  def usageAsked(progName: String, maybeOptions: Either[Error, T]): Nothing = {
    val help = if (progName.isEmpty) finalHelp else finalHelp.copy(progName = progName)
    printLine(help.usage(helpFormat))
    exit(0)
  }

  /** Format of the help messages
    *
    * Defaults to [[caseapp.core.help.HelpFormat.default()* HelpFormat.default()]], which uses ANSI
    * colors. Override it to customize colors, the order of the option groups, etc.
    */
  def helpFormat: HelpFormat =
    HelpFormat.default()

  /** Checks that no two arguments of [[finalHelp]] share the same option name
    *
    * Not called by case-app itself, meant to be called from tests.
    *
    * @throws java.lang.Exception
    *   if some option names are duplicated
    */
  def ensureNoDuplicates(): Unit =
    finalHelp.ensureNoDuplicates()

  /** Arguments are expanded then parsed. By default, argument expansion is the identity function.
    * Overriding this method allows plugging in an arbitrary argument expansion logic.
    *
    * One such expansion logic involves replacing each argument of the form '@<file>' with the
    * contents of that file where each line in the file becomes a distinct argument. To enable this
    * behavior, override this method as shown below.
    *
    * @example
    *   {{{
    * import caseapp.core.parser.PlatformArgsExpander
    * override def expandArgs(args: List[String]): List[String]
    * = PlatformArgsExpander.expand(args)
    *   }}}
    *
    * @param args
    *   arguments passed to `main`
    * @return
    *   arguments to parse
    */
  def expandArgs(args: List[String]): List[String] = args

  /** Whether to stop parsing at the first unrecognized argument.
    *
    * That is, stop parsing at the first non option (not starting with "-"), or the first
    * unrecognized option. The unparsed arguments, including the first unrecognized one, are put in
    * the `remainingArgs` argument of [[run]].
    *
    * `false` by default, in which case non-option arguments can be mixed with options, and
    * unrecognized options are errors (unless [[ignoreUnrecognized]] is `true`).
    */
  def stopAtFirstUnrecognized: Boolean =
    false

  /** Whether to ignore unrecognized arguments.
    *
    * That is, if there are unrecognized options (arguments starting with `-`), the parsing still
    * succeeds. The unrecognized options are put in the `remainingArgs` argument of [[run]], along
    * with non-option arguments.
    *
    * `false` by default, in which case unrecognized options are errors.
    */
  def ignoreUnrecognized: Boolean =
    false

  /** Formats the names of the fields of `T` into option names
    *
    * Defaults to [[caseapp.core.util.Formatter.DefaultNameFormatter]], which converts `camelCase`
    * field names to `kebab-case` (`fooBar` is accepted as `--foo-bar`).
    */
  def nameFormatter: Formatter[Name] =
    Formatter.DefaultNameFormatter

  /** Entry point of the application
    *
    * Parses `args`, and calls [[run]] with the result, or [[error]] in case of error, or prints
    * help messages if they were asked.
    */
  def main(args: Array[String]): Unit =
    main(finalHelp.progName, PlatformUtil.arguments(args))

  /** Parses `args`, and calls [[run]] with the result, or [[error]] in case of error, or prints
    * help messages if they were asked
    *
    * @param progName
    *   program name to use in help messages (when run as a command of a [[CommandsEntryPoint]], the
    *   program name followed by the command name)
    * @param args
    *   arguments to parse, passed to [[expandArgs]] first
    */
  def main(progName: String, args: Array[String]): Unit =
    if (hasFullHelp)
      parser.withFullHelp.detailedParse(
        expandArgs(args.toList),
        stopAtFirstUnrecognized,
        ignoreUnrecognized
      ) match {
        case Left(err)                         => error(err)
        case Right((WithFullHelp(_, true), _)) => fullHelpAsked(progName)
        case Right((WithFullHelp(WithHelp(_, true, maybeOptions), _), _)) =>
          helpAsked(progName, maybeOptions)
        case Right((WithFullHelp(WithHelp(true, _, maybeOptions), _), _)) =>
          usageAsked(progName, maybeOptions)
        case Right((WithFullHelp(WithHelp(_, _, Left(err)), _), _)) => error(err)
        case Right((WithFullHelp(WithHelp(_, _, Right(t)), _), remainingArgs)) =>
          run(t, remainingArgs)
      }
    else if (hasHelp)
      parser.withHelp.detailedParse(
        expandArgs(args.toList),
        stopAtFirstUnrecognized,
        ignoreUnrecognized
      ) match {
        case Left(err)                                        => error(err)
        case Right((WithHelp(_, true, maybeOptions), _))      => helpAsked(progName, maybeOptions)
        case Right((WithHelp(true, _, maybeOptions), _))      => usageAsked(progName, maybeOptions)
        case Right((WithHelp(_, _, Left(err)), _))            => error(err)
        case Right((WithHelp(_, _, Right(t)), remainingArgs)) => run(t, remainingArgs)
      }
    else
      parser.detailedParse(
        expandArgs(args.toList),
        stopAtFirstUnrecognized,
        ignoreUnrecognized
      ) match {
        case Left(err)                 => error(err)
        case Right((t, remainingArgs)) => run(t, remainingArgs)
      }
}

/** Helpers to parse arguments as a `T`, without defining a [[CaseApp]] */
object CaseApp {

  /** Parses `args` like `CaseApp#main` does, and returns the result
    *
    * Accepts the `--help` / `--usage` options. Like `CaseApp#main`, if help is asked, or if `args`
    * can't be parsed, this prints a message and exits the application.
    *
    * @return
    *   the parsed options, and the remaining (non-option) arguments
    */
  def process[T](args: Seq[String])(implicit
    parser: Parser[T],
    help: Help[T]
  ): (T, RemainingArgs) = {
    var values = Option.empty[(T, RemainingArgs)]
    val app: CaseApp[T] = new CaseApp[T] {
      def run(options: T, args: RemainingArgs): Unit = {
        values = Some((options, args))
      }
    }
    app.main(args.toArray)
    values.getOrElse {
      sys.error("should not happen")
    }
  }

  /** Parses `args` as a `T`
    *
    * Help options aren't accepted, see [[parseWithHelp]] for that.
    *
    * {{{
    * CaseApp.parse[Options](Seq("--name", "Alice", "other"))
    * // Right((Options(name = "Alice"), Seq("other")))
    * }}}
    *
    * @return
    *   in case of success, the parsed `T` and the remaining (non-option) arguments, both before and
    *   after a first `--` (see [[caseapp.core.RemainingArgs.all]]); else an error
    */
  def parse[T: Parser](args: Seq[String]): Either[Error, (T, Seq[String])] =
    Parser[T].parse(args)

  /** Parses `args` as a `T`, keeping details about the remaining arguments
    *
    * Like [[parse]], but returns the remaining arguments as a [[caseapp.core.RemainingArgs]], which
    * distinguishes arguments before and after `--`, and keeps their indices.
    */
  def detailedParse[T: Parser](args: Seq[String]): Either[Error, (T, RemainingArgs)] =
    Parser[T].detailedParse(args)

  /** Parses `args` as a `T`, also accepting the `--help` / `-h` and `--usage` options
    *
    * Unlike [[process]], this doesn't print anything, nor exit: it's up to the caller to act on the
    * returned values.
    *
    * @return
    *   `Left` in case of error while parsing the options, like an unrecognized option or a
    *   malformed value. Else, wrapped in `Right`: the result of building the `T` (`Left` if some
    *   mandatory arguments are missing, for example), whether help was asked, whether usage was
    *   asked, and the remaining (non-option) arguments.
    */
  def parseWithHelp[T](args: Seq[String])(implicit
    parser: Parser[T]
  ): Either[Error, (Either[Error, T], Boolean, Boolean, Seq[String])] =
    parser.withHelp.parse(args).map {
      case (WithHelp(usage, help, base), rem) =>
        (base, help, usage, rem)
    }

  /** Like [[parseWithHelp]], but returns the remaining arguments as a
    * [[caseapp.core.RemainingArgs]]
    */
  def detailedParseWithHelp[T](args: Seq[String])(implicit
    parser: Parser[T]
  ): Either[Error, (Either[Error, T], Boolean, Boolean, RemainingArgs)] =
    parser.withHelp.detailedParse(args).map {
      case (WithHelp(usage, help, base), rem) =>
        (base, help, usage, rem)
    }

  /** Help message of `T` (without the help options), see `Help#help`
    */
  def helpMessage[T: Help]: String =
    Help[T].help

  /** One-line usage message of `T`, see `Help#usage` */
  def usageMessage[T: Help]: String =
    Help[T].usage

  /** Prints the help message of `T` (see [[helpMessage]]), on stdout, or on stderr if `err` is
    * `true`
    */
  def printHelp[T: Help](err: Boolean = false): Unit =
    (if (err) Console.err else Console.out) println Help[T].help

  /** Prints the usage message of `T` (see [[usageMessage]]), on stdout, or on stderr if `err` is
    * `true`
    */
  def printUsage[T: Help](err: Boolean = false): Unit =
    (if (err) Console.err else Console.out) println Help[T].usage

}
