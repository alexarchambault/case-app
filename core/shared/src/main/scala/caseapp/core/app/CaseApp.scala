package caseapp.core.app

import caseapp.Name
import caseapp.core.{Error, RemainingArgs}
import caseapp.core.complete.{Completer, CompletionItem, HelpCompleter}
import caseapp.core.help.{Help, WithHelp}
import caseapp.core.parser.Parser
import caseapp.core.util.Formatter

abstract class CaseApp[T](implicit val parser0: Parser[T], val messages: Help[T]) {

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
    parser.withHelp.complete(
      args,
      index,
      completer.withHelp,
      stopAtFirstUnrecognized,
      ignoreUnrecognized
    )

  def run(options: T, remainingArgs: RemainingArgs): Unit

  def exit(code: Int): Nothing =
    sys.exit(code)

  def error(message: Error): Nothing = {
    Console.err.println(message.message)
    exit(1)
  }

  protected def helpWithProgName(progName: String): Help[WithHelp[T]] = {
    val baseHelp = messages.withHelp
    if (progName.isEmpty || progName == baseHelp.progName) baseHelp
    else baseHelp.withProgName(progName)
  }

  def helpAsked(): Nothing =
    helpAsked("")
  def helpAsked(progName: String): Nothing = {
    val help = helpWithProgName(progName)
    println(help.help)
    exit(0)
  }

  def usageAsked(): Nothing =
    usageAsked("")
  def usageAsked(progName: String): Nothing = {
    val help = helpWithProgName(progName)
    println(help.usage)
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

  /**
   * Whether to stop parsing at the first unrecognized argument.
   *
   * That is, stop parsing at the first non option (not starting with "-"), or
   * the first unrecognized option. The unparsed arguments are put in the `args`
   * argument of `run`.
  */
  def stopAtFirstUnrecognized: Boolean =
    false

  /**
    * Whether to ignore unrecognized arguments.
    *
    * That is, if there are unrecognized arguments, the parsing still succeeds.
    * The unparsed arguments are put in the `args` argument of `run`.
    */
  def ignoreUnrecognized: Boolean =
    false

  def nameFormatter: Formatter[Name] =
    Formatter.DefaultNameFormatter

  def main(args: Array[String]): Unit =
    main(messages.progName, args)

  def main(progName: String, args: Array[String]): Unit =
    parser.withHelp.detailedParse(expandArgs(args.toList), stopAtFirstUnrecognized, ignoreUnrecognized) match {
      case Left(err) => error(err)
      case Right((WithHelp(_, true, _), _)) => helpAsked(progName)
      case Right((WithHelp(true, _, _), _)) => usageAsked(progName)
      case Right((WithHelp(_, _, Left(err)), _)) => error(err)
      case Right((WithHelp(_, _, Right(t)), remainingArgs)) => run(t, remainingArgs)
    }
}

object CaseApp {

  def parse[T: Parser](args: Seq[String]): Either[Error, (T, Seq[String])] =
    Parser[T].parse(args)

  def detailedParse[T: Parser](args: Seq[String]): Either[Error, (T, RemainingArgs)] =
    Parser[T].detailedParse(args)

  def parseWithHelp[T](args: Seq[String])(implicit parser: Parser[T]): Either[Error, (Either[Error, T], Boolean, Boolean, Seq[String])] =
    parser.withHelp.parse(args).map {
      case (WithHelp(usage, help, base), rem) =>
        (base, help, usage, rem)
    }

  def detailedParseWithHelp[T](args: Seq[String])(implicit parser: Parser[T]): Either[Error, (Either[Error, T], Boolean, Boolean, RemainingArgs)] =
    parser.withHelp.detailedParse(args).map {
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
