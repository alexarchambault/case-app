package caseapp.cats.app

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
    if (stopAtFirstUnrecognized)
      p.stopAtFirstUnrecognized
    else
      p
  }

  def run(options: T, remainingArgs: RemainingArgs): IO[ExitCode]

  def error(message: Error): IO[ExitCode] = IO {
    Console.err.println(message.message)
    ExitCode.Error
  }

  def helpAsked: IO[ExitCode] = IO {
    println(messages.withHelp.help)
    ExitCode.Success
  }

  def usageAsked: IO[ExitCode] = IO {
    println(messages.withHelp.usage)
    ExitCode.Success
  }

  /**
    * Arguments are expanded then parsed. By default, argument expansion is the identity function.
    * Overriding this method allows plugging in an arbitrary argument expansion logic.
    *
    * One such expansion logic involves replacing each argument of the form '@<file>' with the
    * contents of that file where each line in the file becomes a distinct argument.
    * To enable this behavior, override this method as shown below.

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

  /**
    * Whether to stop parsing at the first unrecognized argument.
    *
    * That is, stop parsing at the first non option (not starting with "-"), or
    * the first unrecognized option. The unparsed arguments are put in the `args`
    * argument of `run`.
    */
  def stopAtFirstUnrecognized: Boolean =
    false

  def nameFormatter: Formatter[Name] =
    Formatter.DefaultNameFormatter

  override def run(args: List[String]): IO[ExitCode] =
    parser.withHelp.detailedParse(args) match {
      case Left(err) => error(err)
      case Right((WithHelp(true, _, _), _)) => usageAsked
      case Right((WithHelp(_, true, _), _)) => helpAsked
      case Right((WithHelp(_, _, Left(err)), _)) => error(err)
      case Right((WithHelp(_, _, Right(t)), remainingArgs)) => run(t, remainingArgs)
    }
}
