package caseapp.core.parser

import scala.language.implicitConversions
import caseapp.core.{Arg, Error}
import caseapp.core.help.WithHelp
import caseapp.core.RemainingArgs
import shapeless.{HList, HNil}
import caseapp.core.util.Formatter
import caseapp.Name

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
  final def step(args: List[String], d: D): Either[(Error, List[String]), Option[(D, List[String])]] =
    step(args, d, defaultNameFormatter)

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
    * @param nameFormatter: formats name to the appropriate format
    * @return if no argument were parsed, `Right(None)`; if an error occurred, an error message wrapped in [[caseapp.core.Error]] and [[scala.Left]]; else the next intermediate value and the remaining arguments wrapped in [[scala.Some]] and [[scala.Right]].
    */
  def step(
      args: List[String],
      d: D,
      nameFormatter: Formatter[Name]
  ): Either[(Error, List[String]), Option[(D, List[String])]]

  /**
    * Get the final result from the final intermediate value.
    *
    * Typically fails if some mandatory arguments were not specified, so are missing in `d`, preventing building a `T`
    * out of it.
    *
    * @param d: final intermediate value
    * @return in case of success, a `T` wrapped in [[scala.Right]]; else, an error message, wrapped in [[caseapp.core.Error]] and [[scala.Left]]
    */
  final def get(d: D): Either[Error, T] = get(d, defaultNameFormatter)

  /**
    * Get the final result from the final intermediate value.
    *
    * Typically fails if some mandatory arguments were not specified, so are missing in `d`, preventing building a `T`
    * out of it.
    *
    * @param d: final intermediate value
    * @param nameFormatter: formats names to the appropriate format
    * @return in case of success, a `T` wrapped in [[scala.Right]]; else, an error message, wrapped in [[caseapp.core.Error]] and [[scala.Left]]
    */
  def get(d: D, nameFormatter: Formatter[Name]): Either[Error, T]

  /**
    * Arguments this parser accepts.
    *
    * Used to generate help / usage messages.
    */
  def args: Seq[Arg]

  def defaultStopAtFirstUnrecognized: Boolean =
    false
  def stopAtFirstUnrecognized: Parser[T] =
    StopAtFirstUnrecognizedParser(this)

  def defaultNameFormatter: Formatter[Name] =
    Formatter.DefaultNameFormatter

  def nameFormatter(f: Formatter[Name]): Parser[T] =
    ParserWithNameFormatter(this, f)

  final def parse(args: Seq[String]): Either[Error, (T, Seq[String])] =
    detailedParse(args)
      .map {
        case (t, rem) =>
          (t, rem.all)
      }

  /** Keeps the remaining args before and after a possible -- separated */
  final def detailedParse(args: Seq[String]): Either[Error, (T, RemainingArgs)] =
    detailedParse(
      args,
      stopAtFirstUnrecognized = defaultStopAtFirstUnrecognized
    )

  final def detailedParse(
    args: Seq[String],
    stopAtFirstUnrecognized: Boolean
  ): Either[Error, (T, RemainingArgs)] = {

    def helper(
      current: D,
      args: List[String],
      extraArgsReverse: List[String]
    ): Either[Error, (T, RemainingArgs)] =
      if (args.isEmpty)
        get(current)
          .map((_, RemainingArgs(extraArgsReverse.reverse, Nil)))
      else
        step(args, current) match {
          case Right(None) =>
            args match {
              case "--" :: rem =>
                get(current)
                  .map { t =>
                    if (stopAtFirstUnrecognized)
                      // extraArgsReverse should be empty anyway here
                      (t, RemainingArgs(extraArgsReverse.reverse ::: args, Nil))
                    else
                      (t, RemainingArgs(extraArgsReverse.reverse, rem))
                  }
              case opt :: rem if opt.startsWith("-") =>
                if (stopAtFirstUnrecognized)
                  get(current)
                    // extraArgsReverse should be empty anyway here
                    .map((_, RemainingArgs(extraArgsReverse.reverse ::: args, Nil)))
                else {
                  val err = Error.UnrecognizedArgument(opt)
                  val remaining = helper(current, rem, extraArgsReverse)
                  Left(remaining.fold(err.append, _ => err))
                }
              case userArg :: rem =>
                if (stopAtFirstUnrecognized)
                  get(current)
                    // extraArgsReverse should be empty anyway here
                    .map((_, RemainingArgs(extraArgsReverse.reverse ::: args, Nil)))
                else
                  helper(current, rem, userArg :: extraArgsReverse)
            }

          case Right(Some((newC, newArgs))) =>

            assert(
              newArgs != args,
              s"From $args, an ArgParser is supposed to have consumed arguments, but returned the same argument list"
            )

            helper(newC, newArgs.toList, extraArgsReverse)

          case Left((msg, rem)) =>
            val remaining = helper(current, rem, extraArgsReverse)
            Left(remaining.fold(errs => msg.append(errs), _ => msg))
        }

    helper(init, args.toList, Nil)
  }

  /**
    * Creates a [[Parser]] accepting help / usage arguments, out of this one.
    */
  final def withHelp: Parser[WithHelp[T]] = {
    implicit val parser: Parser.Aux[T, D] = this
    val p = Parser[WithHelp[T]]
    if (defaultStopAtFirstUnrecognized)
      p.stopAtFirstUnrecognized
    else
      p
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
