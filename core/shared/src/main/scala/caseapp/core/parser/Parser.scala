package caseapp.core.parser

import scala.language.implicitConversions
import caseapp.core.{Arg, Error}
import caseapp.core.help.WithHelp
import caseapp.core.RemainingArgs
import shapeless.{HList, HNil}

/**
  * Parses arguments, resulting in a `T` in case of success.
  *
  * @tparam T: success result type
  */
abstract class Parser[T] {

  /**
    * Intermediate result type.
    *
    * Used during parsing, while checking the arguments one after the other.
    *
    * If parsing succeeds, a `T` can be built from the [[D]] at the end of parsing.
    */
  type D

  /**
    * Initial value used to accumulate parsed arguments.
    */
  def init: D

  /**
    * Process the next argument.
    *
    * If some arguments were successfully processed (third case in return below), the returned remaining argument
    * sequence must be shorter than the passed `args`.
    *
    * This method doesn't fully process `args`. It tries just to parse *one* argument (typically one option `--foo` and
    * its value `bar`, so two elements from `args` - it can also be only one element in case of a flag), if possible.
    * If you want to fully process a sequence of arguments, see `parse` or `detailedParse`.
    *
    * @param args: arguments to process
    * @param d: current intermediate result
    * @return if no argument were parsed, `Right(None)`; if an error occurred, an error message wrapped in [[caseapp.core.Error]] and [[scala.Left]]; else the next intermediate value and the remaining arguments wrapped in [[scala.Some]] and [[scala.Right]].
    */
  def step(args: List[String], d: D): Either[Error, Option[(D, List[String])]]

  /**
    * Get the final result from the final intermediate value.
    *
    * Typically fails if some mandatory arguments were not specified, so are missing in `d`, preventing building a `T`
    * out of it.
    *
    * @param d: final intermediate value
    * @return in case of success, a `T` wrapped in [[scala.Right]]; else, an error message, wrapped in [[caseapp.core.Error]] and [[scala.Left]]
    */
  def get(d: D): Either[Error, T]

  /**
    * Arguments this parser accepts.
    *
    * Used to generate help / usage messages.
    */
  def args: Seq[Arg]


  final def parse(args: Seq[String]): Either[Error, (T, Seq[String])] =
    detailedParse(args)
      .right
      .map {
        case (t, rem) =>
          (t, rem.all)
      }

  /** Keeps the remaining args before and after a possible -- separated */
  final def detailedParse(args: Seq[String]): Either[Error, (T, RemainingArgs)] = {

    def helper(
      current: D,
      args: List[String],
      extraArgsReverse: List[String]
    ): Either[Error, (T, RemainingArgs)] =
      if (args.isEmpty)
        get(current)
          .right
          .map((_, RemainingArgs(extraArgsReverse.reverse, Nil)))
      else
        step(args, current) match {
          case Right(None) =>
            args match {
              case "--" :: t =>
                get(current)
                  .right
                  .map((_, RemainingArgs(extraArgsReverse.reverse, t)))
              case opt :: rem if opt.startsWith("-") => {
                val err = Error.UnrecognizedArgument(opt)
                val remaining: Either[Error, (T, RemainingArgs)] = helper(current, rem, extraArgsReverse)
                Left(remaining.fold(errs => err.append(errs), _ => err))
              }
              case userArg :: rem =>
                helper(current, rem, userArg :: extraArgsReverse)
            }

          case Right(Some((newC, newArgs))) =>

            assert(
              newArgs != args,
              s"From $args, an ArgParser is supposed to have consumed arguments, but returned the same argument list"
            )

            helper(newC, newArgs.toList, extraArgsReverse)

          case Left(msg) => {
            val remaining: Either[Error, (T, RemainingArgs)] = helper(current, args.tail, extraArgsReverse)
            Left(remaining.fold(errs => msg.append(errs), _ => msg))
          }
        }

    helper(init, args.toList, Nil)
  }

  /**
    * Creates a [[Parser]] accepting help / usage arguments, out of this one.
    */
  final def withHelp: Parser[WithHelp[T]] = {
    implicit val parser: Parser.Aux[T, D] = this
    Parser[WithHelp[T]]
  }

  final def map[U](f: T => U): Parser.Aux[U, D] =
    MappedParser(this, f)
}

object Parser extends LowPriorityParserImplicits {

  /** Look for an implicit `Parser[T]` */
  def apply[T](implicit parser: Parser[T]): Aux[T, parser.D] = parser

  type Aux[T, D0] = Parser[T] { type D = D0 }


  /**
    * An empty [[Parser]].
    *
    * Can be made non empty by successively calling `add` on it.
    */
  def nil: Parser.Aux[HNil, HNil] =
    NilParser


  implicit def option[T, D](implicit parser: Aux[T, D]): Parser.Aux[Option[T], D] =
    OptionParser(parser)

  implicit def either[T, D](implicit parser: Aux[T, D]): Parser.Aux[Either[Error, T], D] =
    EitherParser(parser)


  implicit def toParserOps[T <: HList, D <: HList](parser: Aux[T, D]): ParserOps[T, D] =
    new ParserOps(parser)

}
