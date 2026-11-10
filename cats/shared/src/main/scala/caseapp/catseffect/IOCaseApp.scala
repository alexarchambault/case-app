package caseapp.catseffect

import caseapp.core.Error
import caseapp.core.help.{Help, HelpFormat, WithFullHelp, WithHelp}
import caseapp.core.parser.Parser
import caseapp.core.RemainingArgs
import caseapp.Name
import caseapp.core.util.Formatter
import cats.effect.{ExitCode, IO, IOApp}

/** cats-effect version of [[caseapp.core.app.CaseApp]], for single-command applications whose
  * options are a `T`
  *
  * Extend it with an `object`, and implement
  * [[run(options:T,remainingArgs:caseapp.core.RemainingArgs)* run]], which returns an
  * `IO[ExitCode]`. It's an `IOApp`, whose `run` method parses the command-line arguments, handles
  * help options, prints errors, and calls the former with the parsed options.
  *
  * {{{
  * object Hello extends IOCaseApp[Options] {
  *   def run(options: Options, remainingArgs: RemainingArgs): IO[ExitCode] =
  *     IO.println("Hello " + options.name).as(ExitCode.Success)
  * }
  * }}}
  *
  * Unlike [[caseapp.core.app.CaseApp]], it doesn't exit the JVM itself: errors and help requests
  * result in the `IO[ExitCode]` returned by `run`. It doesn't offer shell completions.
  *
  * @param parser0
  *   parser for `T`, usually derived automatically
  * @param messages
  *   help messages for `T`, usually derived automatically
  * @tparam T
  *   type of the options of the application
  */
abstract class IOCaseApp[T](implicit val parser0: Parser[T], val messages: Help[T]) extends IOApp {

  /** Program name, used in help messages, see [[caseapp.core.app.CaseApp.name]] */
  def name: String =
    help.progName

  /** Whether to accept the `--help` / `-h` and `--usage` options, see
    * [[caseapp.core.app.CaseApp.hasHelp]]
    */
  def hasHelp: Boolean = true

  /** Whether to accept the `--help-full` option too, see [[caseapp.core.app.CaseApp.hasFullHelp]]
    */
  def hasFullHelp: Boolean = false

  /** Help messages for `T`, before the help options are added to it */
  def help: Help[T] = messages

  /** Help messages actually printed, see [[caseapp.core.app.CaseApp.finalHelp]] */
  lazy val finalHelp: Help[_] = {
    val h =
      if (hasFullHelp) messages.withFullHelp
      else if (hasHelp) messages.withHelp
      else messages
    if (name == h.progName) h
    else h.copy(progName = name)
  }

  /** Format of the help messages, see [[caseapp.core.app.CaseApp.helpFormat]] */
  def helpFormat: HelpFormat =
    HelpFormat.default()

  private def helpFor(progName: String): Help[_] =
    if (progName.isEmpty) finalHelp else finalHelp.copy(progName = progName)

  /** Parser for `T`, see [[caseapp.core.app.CaseApp.parser]] */
  def parser: Parser[T] = {
    val p = parser0.nameFormatter(nameFormatter)
    if (ignoreUnrecognized)
      p.ignoreUnrecognized
    else if (stopAtFirstUnrecognized)
      p.stopAtFirstUnrecognized
    else
      p
  }

  /** Runs the application, once the arguments have been successfully parsed
    *
    * @param options
    *   parsed options
    * @param remainingArgs
    *   arguments that weren't parsed as options, see [[caseapp.core.RemainingArgs]]
    */
  def run(options: T, remainingArgs: RemainingArgs): IO[ExitCode]

  /** Called when the arguments can't be parsed
    *
    * By default, prints the error message on stderr, and returns `ExitCode.Error`.
    */
  def error(message: Error): IO[ExitCode] =
    IO(Console.err.println(message.message))
      .as(ExitCode.Error)

  /** Called when `--help` or `-h` is passed to a single-command application
    *
    * Prints the help message, and returns `ExitCode.Success`.
    */
  def helpAsked: IO[ExitCode] =
    println(finalHelp.help(helpFormat, showHidden = false))
      .as(ExitCode.Success)

  /** Called when `--usage` is passed to a single-command application
    *
    * Prints the one-line usage message, and returns `ExitCode.Success`.
    */
  def usageAsked: IO[ExitCode] =
    println(finalHelp.usage(helpFormat))
      .as(ExitCode.Success)

  /** Called when `--help-full` is passed, if [[hasFullHelp]] is `true`
    *
    * Prints the full help message (including hidden options), and returns `ExitCode.Success`.
    *
    * @param progName
    *   program name to use in the help message, or empty to use the one of [[finalHelp]]
    */
  def fullHelpAsked(progName: String): IO[ExitCode] =
    println(helpFor(progName).help(helpFormat, showHidden = true))
      .as(ExitCode.Success)

  // When no program name is passed (single-command apps), these delegate to the
  // parameter-less helpAsked / usageAsked, so that overrides of those are still honored
  /** Called when `--help` or `-h` is passed
    *
    * Prints the help message, and returns `ExitCode.Success`.
    *
    * @param progName
    *   program name to use in the help message (when run as a command of an
    *   [[IOCommandsEntryPoint]], the program name followed by the command name), or empty for
    *   single-command applications, in which case this calls the parameter-less `helpAsked`
    */
  def helpAsked(progName: String): IO[ExitCode] =
    if (progName.isEmpty) helpAsked
    else
      println(helpFor(progName).help(helpFormat, showHidden = false))
        .as(ExitCode.Success)

  /** Called when `--usage` is passed
    *
    * Prints the one-line usage message, and returns `ExitCode.Success`.
    *
    * @param progName
    *   program name to use in the usage message (when run as a command of an
    *   [[IOCommandsEntryPoint]], the program name followed by the command name), or empty for
    *   single-command applications, in which case this calls the parameter-less `usageAsked`
    */
  def usageAsked(progName: String): IO[ExitCode] =
    if (progName.isEmpty) usageAsked
    else
      println(helpFor(progName).usage(helpFormat))
        .as(ExitCode.Success)

  /** Prints a line on stdout
    *
    * Help messages are printed with this method. Can be overridden, to capture output in tests for
    * example.
    */
  def println(x: String): IO[Unit] =
    IO(Console.println(x))

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
    * the `remainingArgs` argument of `run`.
    *
    * `false` by default, see [[caseapp.core.app.CaseApp.stopAtFirstUnrecognized]].
    */
  def stopAtFirstUnrecognized: Boolean =
    false

  /** Whether to ignore unrecognized arguments.
    *
    * That is, if there are unrecognized options (arguments starting with `-`), the parsing still
    * succeeds. The unrecognized options are put in the `remainingArgs` argument of `run`, along
    * with non-option arguments.
    *
    * `false` by default.
    */
  def ignoreUnrecognized: Boolean =
    false

  /** Formats the names of the fields of `T` into option names, see
    * [[caseapp.core.app.CaseApp.nameFormatter]]
    */
  def nameFormatter: Formatter[Name] =
    Formatter.DefaultNameFormatter

  /** Entry point of the application, called by cats-effect
    *
    * Parses `args`, and calls the other `run` method with the result, or [[error]] in case of
    * error, or prints help messages if they were asked.
    */
  override def run(args: List[String]): IO[ExitCode] =
    main("", args.toArray)

  /** Parses `args`, and calls `run` with the result, or [[error]] in case of error, or prints help
    * messages if they were asked
    *
    * @param progName
    *   program name to use in help messages (when run as a command of an [[IOCommandsEntryPoint]],
    *   the program name followed by the command name), or empty to use the one of [[finalHelp]]
    * @param args
    *   arguments to parse, passed to [[expandArgs]] first
    */
  def main(progName: String, args: Array[String]): IO[ExitCode] =
    if (hasFullHelp)
      parser.withFullHelp.detailedParse(
        expandArgs(args.toList),
        stopAtFirstUnrecognized,
        ignoreUnrecognized
      ) match {
        case Left(err)                                                   => error(err)
        case Right((WithFullHelp(_, true), _))                           => fullHelpAsked(progName)
        case Right((WithFullHelp(WithHelp(_, true, _), _), _))           => helpAsked(progName)
        case Right((WithFullHelp(WithHelp(true, _, _), _), _))           => usageAsked(progName)
        case Right((WithFullHelp(WithHelp(_, _, Left(err)), _), _))      => error(err)
        case Right((WithFullHelp(WithHelp(_, _, Right(t)), _), remArgs)) => run(t, remArgs)
      }
    else if (hasHelp)
      parser.withHelp.detailedParse(
        expandArgs(args.toList),
        stopAtFirstUnrecognized,
        ignoreUnrecognized
      ) match {
        case Left(err)                                        => error(err)
        case Right((WithHelp(_, true, _), _))                 => helpAsked(progName)
        case Right((WithHelp(true, _, _), _))                 => usageAsked(progName)
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
