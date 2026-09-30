package caseapp.catseffect

import caseapp.core.Error
import caseapp.core.Scala3Helpers._
import caseapp.core.help.{Help, HelpFormat, WithFullHelp, WithHelp}
import caseapp.core.parser.Parser
import caseapp.core.RemainingArgs
import caseapp.Name
import caseapp.core.util.Formatter
import cats.effect.{ExitCode, IO, IOApp}

abstract class IOCaseApp[T](implicit val parser0: Parser[T], val messages: Help[T]) extends IOApp {

  def name: String =
    help.progName

  def hasHelp: Boolean     = true
  def hasFullHelp: Boolean = false

  def help: Help[T] = messages

  lazy val finalHelp: Help[_] = {
    val h =
      if (hasFullHelp) messages.withFullHelp
      else if (hasHelp) messages.withHelp
      else messages
    if (name == h.progName) h
    else h.withProgName(name)
  }

  def helpFormat: HelpFormat =
    HelpFormat.default()

  private def helpFor(progName: String): Help[_] =
    if (progName.isEmpty) finalHelp else finalHelp.withProgName(progName)

  def parser: Parser[T] = {
    val p = parser0.nameFormatter(nameFormatter)
    if (ignoreUnrecognized)
      p.ignoreUnrecognized
    else if (stopAtFirstUnrecognized)
      p.stopAtFirstUnrecognized
    else
      p
  }

  def run(options: T, remainingArgs: RemainingArgs): IO[ExitCode]

  def error(message: Error): IO[ExitCode] =
    IO(Console.err.println(message.message))
      .as(ExitCode.Error)

  def helpAsked: IO[ExitCode] =
    println(finalHelp.help(helpFormat, showHidden = false))
      .as(ExitCode.Success)

  def usageAsked: IO[ExitCode] =
    println(finalHelp.usage(helpFormat))
      .as(ExitCode.Success)

  def fullHelpAsked(progName: String): IO[ExitCode] =
    println(helpFor(progName).help(helpFormat, showHidden = true))
      .as(ExitCode.Success)

  // When no program name is passed (single-command apps), these delegate to the
  // parameter-less helpAsked / usageAsked, so that overrides of those are still honored
  def helpAsked(progName: String): IO[ExitCode] =
    if (progName.isEmpty) helpAsked
    else
      println(helpFor(progName).help(helpFormat, showHidden = false))
        .as(ExitCode.Success)

  def usageAsked(progName: String): IO[ExitCode] =
    if (progName.isEmpty) usageAsked
    else
      println(helpFor(progName).usage(helpFormat))
        .as(ExitCode.Success)

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
    * @return
    */
  def expandArgs(args: List[String]): List[String] = args

  /** Whether to stop parsing at the first unrecognized argument.
    *
    * That is, stop parsing at the first non option (not starting with "-"), or the first
    * unrecognized option. The unparsed arguments are put in the `args` argument of `run`.
    */
  def stopAtFirstUnrecognized: Boolean =
    false

  /** Whether to ignore unrecognized arguments.
    *
    * That is, if there are unrecognized arguments, the parsing still succeeds. The unparsed
    * arguments are put in the `args` argument of `run`.
    */
  def ignoreUnrecognized: Boolean =
    false

  def nameFormatter: Formatter[Name] =
    Formatter.DefaultNameFormatter

  override def run(args: List[String]): IO[ExitCode] =
    main("", args.toArray)

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
