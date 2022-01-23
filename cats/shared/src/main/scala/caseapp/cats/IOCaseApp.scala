package caseapp.cats

import caseapp.core.Error
import caseapp.core.help.{Help, WithHelp}
import caseapp.core.parser.Parser
import caseapp.core.RemainingArgs
import caseapp.Name
import caseapp.core.util.Formatter
import cats.effect.{ExitCode, IO, IOApp}

abstract class IOCaseApp[T](implicit val parser0: Parser[T], val messages: Help[T]) extends IOApp {

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
    println(messages.withHelp.help)
      .as(ExitCode.Success)

  def usageAsked: IO[ExitCode] =
    println(messages.withHelp.usage)
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
    * {{{
    * import caseapp.core.parser.PlatformArgsExpander
    * override def expandArgs(args: List[String]): List[String]
    * = PlatformArgsExpander.expand(args)
    * }}}
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
    parser.withHelp.detailedParse(
      expandArgs(args),
      stopAtFirstUnrecognized,
      ignoreUnrecognized
    ) match {
      case Left(err)                                        => error(err)
      case Right((WithHelp(_, true, _), _))                 => helpAsked
      case Right((WithHelp(true, _, _), _))                 => usageAsked
      case Right((WithHelp(_, _, Left(err)), _))            => error(err)
      case Right((WithHelp(_, _, Right(t)), remainingArgs)) => run(t, remainingArgs)
    }
}
