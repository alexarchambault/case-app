package caseapp.core.parser

import scala.language.implicitConversions
import caseapp.core.{Arg, Error}
import caseapp.core.help.WithHelp
import caseapp.core.RemainingArgs
import shapeless.{HList, HNil}
import caseapp.core.util.Formatter
import caseapp.Name
import caseapp.core.complete.Completer
import caseapp.core.complete.CompletionItem
import scala.annotation.tailrec

/**
  * Parses arguments, resulting in a `T` in case of success.
  *
  * @tparam T: success result type
  */
abstract class Parser[T] {

  import Parser.Step

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

  def step(
      args: List[String],
      d: D
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
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
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]]

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
  def stopAtFirstUnrecognized: Parser.Aux[T, D] =
    StopAtFirstUnrecognizedParser(this)

  def defaultIgnoreUnrecognized: Boolean =
    false
  def ignoreUnrecognized: Parser.Aux[T, D] =
    IgnoreUnrecognizedParser(this)

  def defaultNameFormatter: Formatter[Name] =
    Formatter.DefaultNameFormatter

  def nameFormatter(f: Formatter[Name]): Parser.Aux[T, D] =
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
      stopAtFirstUnrecognized = defaultStopAtFirstUnrecognized,
      ignoreUnrecognized = defaultIgnoreUnrecognized
    )

  final def detailedParse(
    args: Seq[String],
    stopAtFirstUnrecognized: Boolean
  ): Either[Error, (T, RemainingArgs)] =
    detailedParse(
      args,
      stopAtFirstUnrecognized = stopAtFirstUnrecognized,
      ignoreUnrecognized = defaultIgnoreUnrecognized
    )

  final def detailedParse(
    args: Seq[String],
    stopAtFirstUnrecognized: Boolean,
    ignoreUnrecognized: Boolean
  ): Either[Error, (T, RemainingArgs)] = {
    val (res, _) = scan(args, stopAtFirstUnrecognized, ignoreUnrecognized)
    res.left.map(_._1)
  }

  final def scan(
    args: Seq[String],
    stopAtFirstUnrecognized: Boolean,
    ignoreUnrecognized: Boolean
  ): (Either[(Error, Either[D, T]), (T, RemainingArgs)], List[Step]) = {

    def consumed(initial: List[String], updated: List[String]): Int =
      initial match {
        case _ :: tail if tail eq updated => 1
        case _ :: _ :: tail if tail eq updated => 2
        case _ => initial.length - updated.length  // kind of meh, might make parsing O(args.length^2)
      }

    def runHelper(
      current: D,
      args: List[String],
      extraArgsReverse: List[String],
      reverseSteps: List[Step],
      index: Int
    ): (Either[(Error, Either[D, T]), (T, RemainingArgs)], List[Step]) =
      helper(current, args, extraArgsReverse, reverseSteps, index)

    @tailrec
    def helper(
      current: D,
      args: List[String],
      extraArgsReverse: List[String],
      reverseSteps: List[Step],
      index: Int
    ): (Either[(Error, Either[D, T]), (T, RemainingArgs)], List[Step]) = {

      def done = {
        val res = get(current)
          .left.map((_, Left(current)))
          .map((_, RemainingArgs(extraArgsReverse.reverse, Nil)))
        (res, reverseSteps.reverse)
      }

      def stopParsing(tailArgs: List[String]) = {
        val res = get(current)
          .left.map((_, Left(current)))
          .map { t =>
            if (stopAtFirstUnrecognized)
              // extraArgsReverse should be empty anyway here
              (t, RemainingArgs(extraArgsReverse.reverse ::: args, Nil))
            else
              (t, RemainingArgs(extraArgsReverse.reverse, tailArgs))
          }
          val reverseSteps0 = Step.DoubleDash(index) :: reverseSteps.reverse
          (res, reverseSteps0.reverse)
      }

      def unrecognized(headArg: String, tailArgs: List[String]) =
        if (stopAtFirstUnrecognized) {
          val res = get(current)
            .left.map((_, Left(current)))
            // extraArgsReverse should be empty anyway here
            .map((_, RemainingArgs(extraArgsReverse.reverse ::: args, Nil)))
          val reverseSteps0 = Step.FirstUnrecognized(index, isOption = true) :: reverseSteps
          (res, reverseSteps0.reverse)
        } else {
          val err = Error.UnrecognizedArgument(headArg)
          val (remaining, steps) = runHelper(current, tailArgs, extraArgsReverse, Step.Unrecognized(index, err) :: reverseSteps, index + 1)
          val res = Left((remaining.fold(t => err.append(t._1), _ => err), remaining.fold(_._2, t => Right(t._1))))
          (res, steps)
        }

      def stoppingAtUnrecognized = {
        val res = get(current)
          .left.map((_, Left(current)))
          // extraArgsReverse should be empty anyway here
          .map((_, RemainingArgs(extraArgsReverse.reverse ::: args, Nil)))
        val reverseSteps0 = Step.FirstUnrecognized(index, isOption = false) :: reverseSteps
        (res, reverseSteps0.reverse)
      }

      args match {
        case Nil => done
        case headArg :: tailArgs =>
          step(args, current) match {
            case Right(None) =>
              if (headArg == "--")
                stopParsing(tailArgs)
              else if (headArg.startsWith("-") && headArg != "-") {
                if (ignoreUnrecognized)
                  helper(current, tailArgs, headArg :: extraArgsReverse, Step.IgnoredUnrecognized(index) :: reverseSteps, index + 1)
                else
                  unrecognized(headArg, tailArgs)
              } else if (stopAtFirstUnrecognized)
                stoppingAtUnrecognized
              else
                helper(current, tailArgs, headArg :: extraArgsReverse, Step.StandardArgument(index) :: reverseSteps, index + 1)

            case Right(Some((newC, matchedArg, newArgs))) =>

              assert(
                newArgs != args,
                s"From $args, an ArgParser is supposed to have consumed arguments, but returned the same argument list"
              )

              val consumed0 = consumed(args, newArgs)
              assert(consumed0 > 0)

              helper(newC, newArgs.toList, extraArgsReverse, Step.MatchedOption(index, consumed0, matchedArg) :: reverseSteps, index + consumed0)

            case Left((msg, matchedArg, rem)) =>
              val consumed0 = consumed(args, rem)
              assert(consumed0 > 0)
              val (remaining, steps) = runHelper(current, rem, extraArgsReverse, Step.ErroredOption(index, consumed0, matchedArg, msg) :: reverseSteps, index + consumed0)
              val res = Left((remaining.fold(errs => msg.append(errs._1), _ => msg), remaining.fold(_._2, t => Right(t._1))))
              (res, steps)
          }
      }
    }

    helper(init, args.toList, Nil, Nil, 0)
  }

  def complete(
    args: Seq[String],
    index: Int,
    completer: Completer[T],
    stopAtFirstUnrecognized: Boolean,
    ignoreUnrecognized: Boolean
  ): List[CompletionItem] = {

    val args0 = if (index < args.length) args else (args ++ Seq.fill(index + 1 - args.length)(""))

    val (res, steps) = scan(args0, stopAtFirstUnrecognized, ignoreUnrecognized)
    lazy val stateOpt = res match {
      case Left((_, Left(state))) => get(state).toOption
      case Left((_, Right(t))) => Some(t)
      case Right((t, _)) => Some(t)
    }

    assert(index >= 0)
    assert(index < args0.length)

    val prefix = args0(index)

    val stepOpt = steps.find { step =>
      step.index <= index && index < step.index + step.consumed
    }

    val value = args0(index)

    stepOpt match {
      case None => Nil
      case Some(step) =>
        val shift = index - step.index
        step match {
          case Step.DoubleDash(_) =>
            completer.optionName(value, stateOpt)
          case Step.ErroredOption(_, _, _, _) if shift == 0 =>
            completer.optionName(value, stateOpt)
          case Step.ErroredOption(_, consumed, arg, _) if consumed == 2 && shift == 1 =>
            completer.optionValue(arg, value, stateOpt)
          case Step.ErroredOption(_, _, _, _) =>
            // should not happen
            Nil
          case Step.FirstUnrecognized(_, true) =>
            completer.optionName(value, stateOpt)
          case Step.FirstUnrecognized(_, false) =>
            completer.argument(value, stateOpt)
          case Step.IgnoredUnrecognized(_) =>
            completer.optionName(value, stateOpt)
          case Step.Unrecognized(_, _) =>
            completer.optionName(value, stateOpt)
          case Step.StandardArgument(idx) if args0(idx) == "-" =>
            completer.optionName(value, stateOpt)
          case Step.MatchedOption(_, consumed, arg) if shift == 0 =>
            completer.optionName(value, stateOpt)
          case Step.MatchedOption(_, consumed, arg) if consumed == 2 && shift == 1 =>
            completer.optionValue(arg, value, stateOpt)
          case Step.MatchedOption(_, _, _) =>
            // should not happen
            Nil
          case Step.StandardArgument(_) =>
            completer.argument(value, stateOpt)
        }
    }
  }

  /**
    * Creates a [[Parser]] accepting help / usage arguments, out of this one.
    */
  final def withHelp: Parser[WithHelp[T]] = {
    implicit val parser: Parser.Aux[T, D] = this
    val p = ParserWithNameFormatter(Parser[WithHelp[T]], defaultNameFormatter)
    if (defaultIgnoreUnrecognized)
      p.ignoreUnrecognized
    else if (defaultStopAtFirstUnrecognized)
      p.stopAtFirstUnrecognized
    else
      p
  }

  final def map[U](f: T => U): Parser.Aux[U, D] =
    MappedParser(this, f)

  def withDefaultOrigin(origin: String): Parser.Aux[T, D]
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

  sealed abstract class Step extends Product with Serializable {
    def index: Int
    def consumed: Int
  }
  object Step {
    sealed abstract class SingleArg extends Step {
      final def consumed: Int = 1
    }
    final case class DoubleDash(index: Int) extends SingleArg
    final case class IgnoredUnrecognized(index: Int) extends SingleArg
    final case class FirstUnrecognized(index: Int, isOption: Boolean) extends SingleArg
    final case class Unrecognized(index: Int, error: Error.UnrecognizedArgument) extends SingleArg
    final case class StandardArgument(index: Int) extends SingleArg
    final case class MatchedOption(index: Int, consumed: Int, arg: Arg) extends Step
    final case class ErroredOption(index: Int, consumed: Int, arg: Arg, error: Error) extends Step
  }
}
