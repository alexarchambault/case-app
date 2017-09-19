package caseapp.core.commandparser

import scala.language.implicitConversions
import caseapp.core.Error
import caseapp.core.help.WithHelp
import caseapp.core.parser.Parser
import caseapp.core.RemainingArgs
import shapeless.{CNil, Coproduct}

/**
  * Parses arguments, handling sub-commands.
  *
  * @tparam T: result type
  */
abstract class CommandParser[T] {

  /**
    * Check if this [[caseapp.core.commandparser.CommandParser]] accepts a command, and return its [[caseapp.core.parser.Parser]] if it does.
    *
    * @param command: command name to check
    *
    * @return in case of success, `Parser[T]` of `command`, wrapped in [[scala.Some]]; [[scala.None]] in case of failure
    */
  def get(command: String): Option[Parser[T]]

  /**
    * Creates a [[CommandParser]] accepting help / usage arguments, out of this one.
    */
  final def withHelp: CommandParser[WithHelp[T]] =
    WithHelpCommandParser(this)

  /**
    * Parse arguments.
    *
    * Arguments before any command are parsed with `beforeCommandParser` as a `D`.
    *
    * @param args: arguments to parse
    * @param beforeCommandParser: parser for arguments before a command name
    * @tparam D: type for arguments before a command name
    * @return in case of error, an [[caseapp.core.Error]], wrapped in [[scala.Left]]; in case of success, a `D`, the
    *         non option arguments, wrapped in a [[scala.Right]], along with the result of parsing the specified command
    *         if any.
    */
  final def parse[D](
    args: Seq[String]
  )(implicit
    beforeCommandParser: Parser[D]
  ): Either[Seq[Error], (D, Seq[String], Option[Either[Seq[Error], (String, T, Seq[String])]])] =
    detailedParse(args)
      .right
      .map {
        case (d, args0, cmdOpt) =>
          (d, args0, cmdOpt.map(_.right.map {
            case (cmd, t, rem) =>
              (cmd, t, rem.all)
          }))
      }

  /**
    * Parse arguments.
    *
    * Like `parse`, but keeps the non option arguments in a [[caseapp.core.RemainingArgs]].
    *
    * @param args: arguments to parse
    * @param beforeCommandParser: parser for arguments before a command name
    * @tparam D: type for arguments before a command name
    * @return in case of error, an [[caseapp.core.Error]], wrapped in [[scala.Left]]; in case of success, a `D`, the
    *         non option arguments, wrapped in a [[scala.Right]], along with the result of parsing the specified command
    *         if any.
    */
  final def detailedParse[D](
    args: Seq[String]
  )(implicit
    beforeCommandParser: Parser[D]
  ): Either[Seq[Error], (D, Seq[String], Option[Either[Seq[Error], (String, T, RemainingArgs)]])] = {

    def helper(
      current: beforeCommandParser.D,
      args: List[String]
    ): Either[Seq[Error], (D, RemainingArgs)] =
      if (args.isEmpty)
        beforeCommandParser
          .get(current)
          .right
          .map((_, RemainingArgs(Nil, args)))
      else
        beforeCommandParser.step(args, current) match {
          case Right(None) =>
            args match {
              case "--" :: t =>
                beforeCommandParser.get(current).right.map((_, RemainingArgs(t, Nil)))
              case opt :: rem if opt startsWith "-" => {
                val err = Error.UnrecognizedArgument(opt)
                val remaining: Either[Seq[Error], (D, RemainingArgs)] = helper(current, rem)
                Left(remaining.fold(errs => err +: errs, _ => Seq(err)))
              }
              case rem =>
                beforeCommandParser.get(current).right.map((_, RemainingArgs(Nil, rem)))
            }

          case Right(Some((newD, newArgs))) =>
            assert(newArgs != args)
            helper(newD, newArgs)

          case Left(msg) => {
            val remaining: Either[Seq[Error], (D, RemainingArgs)] = helper(current, args.tail)
            Left(remaining.fold(errs => msg +: errs, _ => Seq(msg)))
          }
        }

    helper(beforeCommandParser.init, args.toList)
      .right
      .map {
        case (d, dArgs) =>
          val cmdOpt = dArgs.unparsed.toList match {
            case c :: rem0 =>
              get(c) match {
                case None =>
                  Some(Left(Seq(Error.CommandNotFound(c))))
                case Some(p) =>
                  Some(
                    p
                      .detailedParse(rem0)
                      .right
                      .map {
                        case (t, trem) =>
                          (c, t, trem)
                      }
                  )
              }
            case Nil =>
              None
          }

          (d, dArgs.remaining, cmdOpt)
      }
  }

  final def map[U](f: T => U): CommandParser[U] =
    MappedCommandParser(this, f)

}

object CommandParser extends AutoCommandParserImplicits {

  def apply[T](implicit parser: CommandParser[T]): CommandParser[T] = parser

  /**
    * An empty [[CommandParser]].
    *
    * Can be made non empty by successively calling `add` on it.
    */
  def nil: CommandParser[CNil] =
    NilCommandParser

  implicit def toCommandParserOps[T <: Coproduct](parser: CommandParser[T]): CommandParserOps[T] =
    new CommandParserOps(parser)

}
