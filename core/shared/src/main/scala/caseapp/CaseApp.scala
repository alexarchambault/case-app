package caseapp

import caseapp.core._

object CaseApp {

  def parse[T: Parser](args: Seq[String]): Either[String, (T, Seq[String])] =
    Parser[T].parse(args)

  def detailedParse[T: Parser](args: Seq[String]): Either[String, (T, Seq[String], Seq[String])] =
    Parser[T].detailedParse(args)

  def parseWithHelp[T](args: Seq[String])(implicit parser: Parser[T]): Either[String, (T, Boolean, Boolean, Seq[String])] =
    parser.withHelp.parse(args).right map {
      case (WithHelp(usage, help, base), rem) =>
        (base, help, usage, rem)
    }

  def detailedParseWithHelp[T](args: Seq[String])(implicit parser: Parser[T]): Either[String, (T, Boolean, Boolean, Seq[String], Seq[String])] =
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
