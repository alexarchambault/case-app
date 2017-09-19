package caseapp.core.app

import caseapp.core.Error
import caseapp.core.help.{Help, WithHelp}
import caseapp.core.parser.Parser
import caseapp.core.RemainingArgs

abstract class CaseApp[T](implicit val parser: Parser[T], val messages: Help[T]) {

  def run(options: T, remainingArgs: RemainingArgs): Unit

  def exit(code: Int): Nothing =
    sys.exit(code)

  def error(messages: Seq[Error]): Nothing = {
    messages.foreach(message => Console.err.println(message.message))
    exit(1)
  }

  def helpAsked(): Nothing = {
    println(messages.withHelp.help)
    exit(0)
  }

  def usageAsked(): Nothing = {
    println(messages.withHelp.usage)
    exit(0)
  }

  def main(args: Array[String]): Unit =
    parser.withHelp.detailedParse(args) match {
      case Left(errs) =>
        error(errs)

      case Right((WithHelp(usage, help, t), remainingArgs)) =>

        if (help)
          helpAsked()

        if (usage)
          usageAsked()

        t.fold(
          error,
          run(_, remainingArgs)
        )
    }
}

object CaseApp {

  def parse[T: Parser](args: Seq[String]): Either[Seq[Error], (T, Seq[String])] =
    Parser[T].parse(args)

  def detailedParse[T: Parser](args: Seq[String]): Either[Seq[Error], (T, RemainingArgs)] =
    Parser[T].detailedParse(args)

  def parseWithHelp[T](args: Seq[String])(implicit parser: Parser[T]): Either[Seq[Error], (Either[Seq[Error], T], Boolean, Boolean, Seq[String])] =
    parser.withHelp.parse(args).right.map {
      case (WithHelp(usage, help, base), rem) =>
        (base, help, usage, rem)
    }

  def detailedParseWithHelp[T](args: Seq[String])(implicit parser: Parser[T]): Either[Seq[Error], (Either[Seq[Error], T], Boolean, Boolean, RemainingArgs)] =
    parser.withHelp.detailedParse(args).right map {
      case (WithHelp(usage, help, base), rem) =>
        (base, help, usage, rem)
    }

  def helpMessage[T: Help]: String =
    Help[T].help

  def usageMessage[T: Help]: String =
    Help[T].usage

  def printHelp[T: Help](err: Boolean = false): Unit =
    (if (err) Console.err else Console.out) println Help[T].help

  def printUsage[T: Help](err: Boolean = false): Unit =
    (if (err) Console.err else Console.out) println Help[T].usage

}
