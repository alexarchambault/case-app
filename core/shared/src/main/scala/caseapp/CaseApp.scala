package caseapp

import caseapp.core._

abstract class CaseApp[T](implicit val parser: Parser[T], val messages: Messages[T]) {

  def run(options: T, remainingArgs: RemainingArgs): Unit

  def exit(code: Int): Unit =
    sys.exit(code)

  def error(message: String): Unit = {
    Console.err.println(message)
    exit(1)
  }

  def helpAsked(): Unit = {
    println(messages.withHelp.helpMessage)
    exit(0)
  }

  def usageAsked(): Unit = {
    println(messages.withHelp.usageMessage)
    exit(0)
  }

  def main(args: Array[String]): Unit =
    parser.withHelp.detailedParse(args) match {
      case Left(err) =>
        error(err)

      case Right((WithHelp(usage, help, t), remainingArgs, extraArgs)) =>

        if (help)
          helpAsked()

        if (usage)
          usageAsked()

        t match {
          case Left(err) =>
            error(err)
          case Right(u) =>
            run(u, RemainingArgs(remainingArgs, extraArgs))
        }
    }
}

object CaseApp {

  def parse[T: Parser](args: Seq[String]): Either[String, (T, Seq[String])] =
    Parser[T].parse(args)

  def detailedParse[T: Parser](args: Seq[String]): Either[String, (T, Seq[String], Seq[String])] =
    Parser[T].detailedParse(args)

  def parseWithHelp[T](args: Seq[String])(implicit parser: Parser[T]): Either[String, (Either[String, T], Boolean, Boolean, Seq[String])] =
    parser.withHelp.parse(args).right.map {
      case (WithHelp(usage, help, base), rem) =>
        (base, help, usage, rem)
    }

  def detailedParseWithHelp[T](args: Seq[String])(implicit parser: Parser[T]): Either[String, (Either[String, T], Boolean, Boolean, Seq[String], Seq[String])] =
    parser.withHelp.detailedParse(args).right map {
      case (WithHelp(usage, help, base), rem, extra) =>
        (base, help, usage, rem, extra)
    }

  def helpMessage[T: Messages]: String =
    Messages[T].helpMessage

  def usageMessage[T: Messages]: String =
    Messages[T].usageMessage

  def printHelp[T: Messages](err: Boolean = false): Unit =
    (if (err) Console.err else Console.out) println Messages[T].helpMessage

  def printUsage[T: Messages](err: Boolean = false): Unit =
    (if (err) Console.err else Console.out) println Messages[T].usageMessage

}
