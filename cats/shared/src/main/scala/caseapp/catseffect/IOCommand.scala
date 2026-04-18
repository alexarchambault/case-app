package caseapp.catseffect

import caseapp.core.Error
import caseapp.core.RemainingArgs
import caseapp.core.Scala3Helpers._
import caseapp.core.help.{Help, HelpFormat, WithHelp}
import caseapp.core.parser.Parser
import caseapp.Name
import caseapp.core.util.Formatter
import caseapp.core.complete.{Completer, CompletionItem, HelpCompleter}
import cats.effect.{ExitCode, IO}

abstract class IOCommand[T](implicit val parser0: Parser[T], val messages: Help[T]) {

  def names: List[List[String]] =
    List(List(name))
  def group: String   = ""
  def hidden: Boolean = false

  def name: String =
    messages.progName

  def hasHelp: Boolean     = true
  def hasFullHelp: Boolean = false

  def help: Help[T] = messages

  def parser: Parser[T] = {
    val p = parser0.nameFormatter(nameFormatter)
    if (ignoreUnrecognized)
      p.ignoreUnrecognized
    else if (stopAtFirstUnrecognized)
      p.stopAtFirstUnrecognized
    else
      p
  }

  def completer: Completer[T] =
    new HelpCompleter[T](messages)

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

  def run(options: T, remainingArgs: RemainingArgs): IO[ExitCode]

  def error(message: Error): IO[ExitCode] =
    IO(Console.err.println(message.message))
      .as(ExitCode.Error)

  def helpAsked(progName: String): IO[ExitCode] = {
    val h =
      if (progName.isEmpty) finalHelp
      else finalHelp.withProgName(progName)
    println(h.help(helpFormat, showHidden = false))
      .as(ExitCode.Success)
  }

  def usageAsked(progName: String): IO[ExitCode] = {
    val h =
      if (progName.isEmpty) finalHelp
      else finalHelp.withProgName(progName)
    println(h.usage(helpFormat))
      .as(ExitCode.Success)
  }

  def println(x: String): IO[Unit] =
    IO(Console.println(x))

  lazy val finalHelp: Help[_] =
    if (hasFullHelp) messages.withFullHelp
    else if (hasHelp) messages.withHelp
    else messages

  def helpFormat: HelpFormat =
    HelpFormat.default()

  def expandArgs(args: List[String]): List[String] = args

  def stopAtFirstUnrecognized: Boolean =
    false

  def ignoreUnrecognized: Boolean =
    false

  def nameFormatter: Formatter[Name] =
    Formatter.DefaultNameFormatter

  def main(progName: String, args: Array[String]): IO[ExitCode] =
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
}
