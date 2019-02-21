package caseapp.core.app

import caseapp.core.Error
import caseapp.core.help.{Help, WithHelp}
import caseapp.core.parser.Parser
import caseapp.core.RemainingArgs

abstract class CaseApp[T](implicit val parser: Parser[T], val messages: Help[T]) {

  def run(options: T, remainingArgs: RemainingArgs): Unit

  def exit(code: Int): Nothing =
    sys.exit(code)

  def error(message: Error): Nothing = {
    Console.err.println(message.message)
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

  def ensureNoDuplicates(): Unit =
    messages.ensureNoDuplicates()

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

  def main(args: Array[String]): Unit =
    parser.withHelp.detailedParse(expandArgs(args.toList)) match {
      case Left(err) =>
        error(err)

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

  def parse[T: Parser](args: Seq[String]): Either[Error, (T, Seq[String])] =
    Parser[T].parse(args)

  def detailedParse[T: Parser](args: Seq[String]): Either[Error, (T, RemainingArgs)] =
    Parser[T].detailedParse(args)

  def parseWithHelp[T](args: Seq[String])(implicit parser: Parser[T]): Either[Error, (Either[Error, T], Boolean, Boolean, Seq[String])] =
    parser.withHelp.parse(args).right.map {
      case (WithHelp(usage, help, base), rem) =>
        (base, help, usage, rem)
    }

  def detailedParseWithHelp[T](args: Seq[String])(implicit parser: Parser[T]): Either[Error, (Either[Error, T], Boolean, Boolean, RemainingArgs)] =
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
