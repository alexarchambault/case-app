package caseapp.cats.app

import caseapp.core.Error
import caseapp.core.help.{Help, WithHelp}
import caseapp.core.parser.Parser
import caseapp.core.RemainingArgs
import cats.effect.{ExitCode, IO, IOApp}

abstract class IOCaseApp[T](implicit val parser: Parser[T], val messages: Help[T]) extends IOApp {
  def run(options: T, remainingArgs: RemainingArgs): IO[ExitCode]

  def error(message: Error): IO[ExitCode] = IO {
    Console.err.println(message.message)
    ExitCode(1)
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

  override def run(args: List[String]): IO[ExitCode] =
    parser.withHelp.detailedParse(expandArgs(args)) match {
      case Left(err) =>
        error(err)
      case Right((WithHelp(true, _, _), _)) =>
        usageAsked
      case Right((WithHelp(_, true, _), _)) =>
        helpAsked
      case Right((WithHelp(_, _, t), remainingArgs)) =>
        t.fold(
          error,
          run(_, remainingArgs)
        )
    }
}
