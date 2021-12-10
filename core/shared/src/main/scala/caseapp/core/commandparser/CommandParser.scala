package caseapp.core.commandparser

import scala.language.implicitConversions
import caseapp.core.{Error, Indexed}
import caseapp.core.help.WithHelp
import caseapp.core.parser.Parser
import caseapp.core.RemainingArgs
import shapeless.{CNil, Coproduct}

import scala.collection.mutable
import scala.annotation.tailrec

/** Parses arguments, handling sub-commands.
  *
  * @tparam T:
  *   result type
  */
@deprecated("Use Command and CommandsEntryPoint instead", "2.1.0")
abstract class CommandParser[T] {

  /** Check if this [[caseapp.core.commandparser.CommandParser]] accepts a command, and return its
    * [[caseapp.core.parser.Parser]] if it does.
    *
    * @param command:
    *   command name to check
    *
    * @return
    *   in case of success, `Parser[T]` of `command`, wrapped in [[scala.Some]]; [[scala.None]] in
    *   case of failure
    */
  def get(command: Seq[String]): Option[Parser[T]] =
    commandMap.get(command)

  private lazy val commandTree = CommandParser.CommandTree.fromCommandMap(commandMap)

  def commandMap: Map[Seq[String], Parser[T]]

  /** Creates a [[CommandParser]] accepting help / usage arguments, out of this one.
    */
  final def withHelp: CommandParser[WithHelp[T]] =
    WithHelpCommandParser(this)

  /** Parse arguments.
    *
    * Arguments before any command are parsed with `beforeCommandParser` as a `D`.
    *
    * @param args:
    *   arguments to parse
    * @param beforeCommandParser:
    *   parser for arguments before a command name
    * @tparam D:
    *   type for arguments before a command name
    * @return
    *   in case of error, an [[caseapp.core.Error]], wrapped in [[scala.Left]]; in case of success,
    *   a `D`, the non option arguments, wrapped in a [[scala.Right]], along with the result of
    *   parsing the specified command if any.
    */
  final def parse[D](
    args: Seq[String]
  )(implicit
    beforeCommandParser: Parser[D]
  ): Either[Error, (D, Seq[String], Option[Either[Error, (Seq[String], T, Seq[String])]])] =
    detailedParse(args).map {
      case (d, args0, cmdOpt) =>
        (
          d,
          args0,
          cmdOpt.map(_.map {
            case (cmd, t, rem) =>
              (cmd, t, rem.all)
          })
        )
    }

  /** Parse arguments.
    *
    * Like `parse`, but keeps the non option arguments in a [[caseapp.core.RemainingArgs]].
    *
    * @param args:
    *   arguments to parse
    * @param beforeCommandParser:
    *   parser for arguments before a command name
    * @tparam D:
    *   type for arguments before a command name
    * @return
    *   in case of error, an [[caseapp.core.Error]], wrapped in [[scala.Left]]; in case of success,
    *   a `D`, the non option arguments, wrapped in a [[scala.Right]], along with the result of
    *   parsing the specified command if any.
    */
  final def detailedParse[D](
    args: Seq[String]
  )(implicit
    beforeCommandParser: Parser[D]
  ): Either[Error, (D, Seq[String], Option[Either[Error, (Seq[String], T, RemainingArgs)]])] = {

    def helper(
      current: beforeCommandParser.D,
      args: List[String],
      index: Int
    ): Either[Error, (D, RemainingArgs)] =
      if (args.isEmpty)
        beforeCommandParser
          .get(current)
          .map((_, RemainingArgs(Nil, Nil)))
      else
        beforeCommandParser.step(args, index, current) match {
          case Right(None) =>
            args match {
              case "--" :: t =>
                beforeCommandParser.get(current).map((
                  _,
                  RemainingArgs(Indexed.seq(t, index + 1), Nil)
                ))
              case opt :: rem if opt startsWith "-" =>
                val err                                          = Error.UnrecognizedArgument(opt)
                val remaining: Either[Error, (D, RemainingArgs)] = helper(current, rem, index)
                Left(remaining.fold(errs => err.append(errs), _ => err))
              case rem =>
                beforeCommandParser.get(current).map((
                  _,
                  RemainingArgs(Nil, Indexed.seq(rem, index))
                ))
            }

          case Right(Some((newD, _, newArgs))) =>
            assert(newArgs != args)
            val consumed = Parser.consumed(args, newArgs)
            assert(consumed > 0)
            helper(newD, newArgs, index + consumed)

          case Left((msg, _, rem)) =>
            val remaining = helper(current, rem, index)
            Left(remaining.fold(errs => msg.append(errs), _ => msg))
        }

    helper(beforeCommandParser.init, args.toList, 0).map {
      case (d, dArgs) =>
        val args0 = dArgs.unparsed.toList

        val cmdOpt =
          if (args0.isEmpty) None
          else
            commandTree.command(args0) match {
              case Some((cmd, p, rem0)) =>
                Some(
                  p
                    .detailedParse(rem0)
                    .map {
                      case (t, trem) =>
                        (cmd, t, trem)
                    }
                )
              case None =>
                Some(Left(Error.CommandNotFound(args0.head)))
            }

        (d, dArgs.remaining, cmdOpt)
    }
  }

  final def map[U](f: T => U): CommandParser[U] =
    MappedCommandParser(this, f)

}

@deprecated("Use Command and CommandsEntryPoint instead", "2.1.0")
object CommandParser extends AutoCommandParserImplicits {

  def apply[T](implicit parser: CommandParser[T]): CommandParser[T] = parser

  /** An empty [[CommandParser]].
    *
    * Can be made non empty by successively calling `add` on it.
    */
  def nil: CommandParser[CNil] =
    NilCommandParser

  implicit def toCommandParserOps[T <: Coproduct](parser: CommandParser[T]): CommandParserOps[T] =
    new CommandParserOps(parser)

  private final case class CommandTree[T](map: Map[String, (CommandTree[T], Option[Parser[T]])]) {

    def command(args: Seq[String]): Option[(Seq[String], Parser[T], Seq[String])] =
      command(args, Nil)

    def command(
      args: Seq[String],
      reverseName: List[String]
    ): Option[(Seq[String], Parser[T], Seq[String])] = {
      assert(args.nonEmpty)
      map.get(args.head) match {
        case None => None
        case Some((tree0, parserOpt)) =>
          val reverseName0 = args.head :: reverseName
          lazy val current = parserOpt.map((reverseName0.reverse, _, args.tail))
          if (args.lengthCompare(1) == 0)
            current
          else
            tree0.command(args.tail, reverseName0).orElse(current)
      }
    }
  }

  private object CommandTree {
    private final case class Mutable[T](
      map: mutable.HashMap[String, (Mutable[T], Option[Parser[T]])] =
        new mutable.HashMap[String, (Mutable[T], Option[Parser[T]])]
    ) {
      @tailrec
      def add(command: Seq[String], parser: Parser[T]): Unit = {
        assert(command.nonEmpty)
        if (command.lengthCompare(1) == 0) {
          val mutable0 = map.get(command.head).map(_._1).getOrElse(Mutable[T]())
          map.put(command.head, (mutable0, Some(parser)))
        }
        else {
          val (mutable0, _) = map.getOrElseUpdate(command.head, (Mutable[T](), None))
          mutable0.add(command.tail, parser)
        }
      }

      def add(commandMap: Map[Seq[String], Parser[T]]): this.type = {
        for ((c, p) <- commandMap)
          add(c, p)
        this
      }

      def result: CommandTree[T] = {
        val map0 = map
          .iterator
          .map {
            case (name, (mutable0, parserOpt)) =>
              (name, (mutable0.result, parserOpt))
          }
          .toMap
        CommandTree(map0)
      }
    }

    def fromCommandMap[T](commandMap: Map[Seq[String], Parser[T]]): CommandTree[T] =
      Mutable[T]().add(commandMap).result
  }

}
