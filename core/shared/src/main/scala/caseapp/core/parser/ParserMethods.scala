package caseapp.core.parser

import caseapp.core.{Arg, Error, Indexed}
import caseapp.core.RemainingArgs
import caseapp.core.util.Formatter
import caseapp.Name
import caseapp.core.complete.Completer
import caseapp.core.complete.CompletionItem

import scala.annotation.tailrec

/** Methods of `Parser`, parsing arguments as a `T`
  *
  * The main ones are `parse` and `detailedParse`, that parse a whole sequence of arguments. They
  * rely on `init`, `step`, and `get`, which implementations of `Parser` define.
  */
trait ParserMethods[+T] { parser: Parser[T @Internal.uncheckedVarianceScala2] =>

  import Parser.Step

  /** Initial value used to accumulate parsed arguments.
    */
  def init: D

  /** Process the next argument, using `defaultNameFormatter` to format option names
    *
    * See the other `step` method for more details.
    */
  def step(
    args: List[String],
    index: Int,
    d: D
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
    step(args, index, d, defaultNameFormatter)

  /** Process the next argument.
    *
    * If some arguments were successfully processed (third case in return below), the returned
    * remaining argument sequence must be shorter than the passed `args`.
    *
    * This method doesn't fully process `args`. It tries just to parse *one* argument (typically one
    * option `--foo` and its value `bar`, so two elements from `args` - it can also be only one
    * element in case of a flag), if possible. If you want to fully process a sequence of arguments,
    * see `parse` or `detailedParse`.
    *
    * @param args:
    *   arguments to process
    * @param index:
    *   index of the first element of `args`, in the whole sequence of arguments being parsed
    * @param d:
    *   current intermediate result
    * @param nameFormatter:
    *   formats name to the appropriate format
    * @return
    *   if no argument were parsed, `Right(None)`; if an error occurred, an error message wrapped in
    *   `Error` and `Left`, along with the argument that failed and the remaining arguments; else
    *   the next intermediate value, the matched argument, and the remaining arguments wrapped in
    *   `Some` and `Right`.
    */
  def step(
    args: List[String],
    index: Int,
    d: D,
    nameFormatter: Formatter[Name]
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]]

  /** Get the final result from the final intermediate value.
    *
    * Typically fails if some mandatory arguments were not specified, so are missing in `d`,
    * preventing building a `T` out of it.
    *
    * @param d:
    *   final intermediate value
    * @return
    *   in case of success, a `T` wrapped in `Right`; else, an error message, wrapped in `Error` and
    *   `Left`
    */
  final def get(d: D): Either[Error, T] = get(d, defaultNameFormatter)

  /** Get the final result from the final intermediate value.
    *
    * Typically fails if some mandatory arguments were not specified, so are missing in `d`,
    * preventing building a `T` out of it.
    *
    * @param d:
    *   final intermediate value
    * @param nameFormatter:
    *   formats names to the appropriate format
    * @return
    *   in case of success, a `T` wrapped in `Right`; else, an error message, wrapped in `Error` and
    *   `Left`
    */
  def get(d: D, nameFormatter: Formatter[Name]): Either[Error, T]

  /** Arguments this parser accepts.
    *
    * Used to generate help / usage messages.
    */
  def args: Seq[Arg]

  /** Whether `parse` and `detailedParse` stop at the first unrecognized argument by default
    *
    * `false` by default, `true` for parsers created with `Parser.stopAtFirstUnrecognized`.
    */
  def defaultStopAtFirstUnrecognized: Boolean =
    false

  /** Whether `parse` and `detailedParse` ignore unrecognized options by default
    *
    * `false` by default, `true` for parsers created with `Parser.ignoreUnrecognized`.
    */
  def defaultIgnoreUnrecognized: Boolean =
    false

  /** Formatter used to compute option names from field names, see `Parser.nameFormatter`
    *
    * Defaults to [[caseapp.core.util.Formatter.DefaultNameFormatter]].
    */
  def defaultNameFormatter: Formatter[Name] =
    Formatter.DefaultNameFormatter

  /** Parses `args`
    *
    * @return
    *   in case of success, the parsed `T`, and the arguments that weren't parsed as options (both
    *   before and after a first `--`, see [[caseapp.core.RemainingArgs.all]]); else, an error,
    *   possibly aggregating several errors (see [[caseapp.core.Error.SeveralErrors]])
    */
  final def parse(args: Seq[String]): Either[Error, (T, Seq[String])] =
    detailedParse(args)
      .map {
        case (t, rem) =>
          (t, rem.all)
      }

  /** Parses `args`, keeping the remaining args before and after a possible `--` separated
    *
    * Like `parse`, but returns the remaining arguments as a [[caseapp.core.RemainingArgs]].
    */
  final def detailedParse(args: Seq[String]): Either[Error, (T, RemainingArgs)] =
    detailedParse(
      args,
      stopAtFirstUnrecognized = defaultStopAtFirstUnrecognized,
      ignoreUnrecognized = defaultIgnoreUnrecognized
    )

  /** Parses `args`, possibly stopping at the first unrecognized argument
    *
    * See the other `detailedParse` methods.
    */
  final def detailedParse(
    args: Seq[String],
    stopAtFirstUnrecognized: Boolean
  ): Either[Error, (T, RemainingArgs)] =
    detailedParse(
      args,
      stopAtFirstUnrecognized = stopAtFirstUnrecognized,
      ignoreUnrecognized = defaultIgnoreUnrecognized
    )

  /** Parses `args`
    *
    * @param stopAtFirstUnrecognized
    *   whether to stop parsing at the first unrecognized argument (the first argument not starting
    *   with `-`, or the first unrecognized option), putting it and all the arguments that follow in
    *   the remaining arguments
    * @param ignoreUnrecognized
    *   whether to put unrecognized options in the remaining arguments, rather than failing
    * @return
    *   in case of success, the parsed `T`, and the remaining arguments; else, an error
    */
  final def detailedParse(
    args: Seq[String],
    stopAtFirstUnrecognized: Boolean,
    ignoreUnrecognized: Boolean
  ): Either[Error, (T, RemainingArgs)] = {
    val (res, remArgs, _) = scan(args, stopAtFirstUnrecognized, ignoreUnrecognized)
    res
      .left.map(_._1)
      .map((_, remArgs))
  }

  /** Parses `args`, also returning the details of each parsing step
    *
    * Lower-level than `detailedParse`, and mostly meant to compute completions (see `complete`).
    *
    * @return
    *   the parsing result (in case of error, along with the last intermediate value, or the `T` if
    *   it could be built), the remaining arguments, and the parsing steps
    */
  final def scan(
    args: Seq[String],
    stopAtFirstUnrecognized: Boolean,
    ignoreUnrecognized: Boolean
  ): (Either[(Error, Either[D, T]), T], RemainingArgs, List[Step]) = {

    def runHelper(
      current: D,
      args: List[String],
      extraArgsReverse: List[Indexed[String]],
      reverseSteps: List[Step],
      index: Int
    ): (Either[(Error, Either[D, T]), T], RemainingArgs, List[Step]) =
      helper(current, args, extraArgsReverse, reverseSteps, index)

    @tailrec
    def helper(
      current: D,
      args: List[String],
      extraArgsReverse: List[Indexed[String]],
      reverseSteps: List[Step],
      index: Int
    ): (Either[(Error, Either[D, T]), T], RemainingArgs, List[Step]) = {

      def done = {
        val remArgs = RemainingArgs(extraArgsReverse.reverse, Nil)
        val res = get(current)
          .left.map((_, Left(current)))
        (res, remArgs, reverseSteps.reverse)
      }

      def stopParsing(tailArgs: List[String]) = {
        val remArgs =
          if (stopAtFirstUnrecognized)
            // extraArgsReverse should be empty anyway here
            RemainingArgs(extraArgsReverse.reverse ::: Indexed.list(args, index), Nil)
          else
            RemainingArgs(extraArgsReverse.reverse, Indexed.seq(tailArgs, index + 1))
        val res = get(current)
          .left.map((_, Left(current)))
        val reverseSteps0 = Step.DoubleDash(index) :: reverseSteps.reverse
        (res, remArgs, reverseSteps0.reverse)
      }

      def unrecognized(headArg: String, tailArgs: List[String]) =
        if (stopAtFirstUnrecognized) {
          // extraArgsReverse should be empty anyway here
          val remArgs = RemainingArgs(extraArgsReverse.reverse ::: Indexed.list(args, index), Nil)
          val res = get(current)
            .left.map((_, Left(current)))
          val reverseSteps0 = Step.FirstUnrecognized(index, isOption = true) :: reverseSteps
          (res, remArgs, reverseSteps0.reverse)
        }
        else {
          val err = Error.UnrecognizedArgument(headArg)
          val (remaining, remArgs, steps) = runHelper(
            current,
            tailArgs,
            extraArgsReverse,
            Step.Unrecognized(index, err) :: reverseSteps,
            index + 1
          )
          val res = Left((
            remaining.fold(t => err.append(t._1), _ => err),
            remaining.fold(_._2, Right(_))
          ))
          (res, remArgs, steps)
        }

      def stoppingAtUnrecognized = {
        // extraArgsReverse should be empty anyway here
        val remArgs = RemainingArgs(extraArgsReverse.reverse ::: Indexed.list(args, index), Nil)
        val res = get(current)
          .left.map((_, Left(current)))
        val reverseSteps0 = Step.FirstUnrecognized(index, isOption = false) :: reverseSteps
        (res, remArgs, reverseSteps0.reverse)
      }

      args match {
        case Nil => done
        case headArg :: tailArgs =>
          step(args, index, current) match {
            case Right(None) =>
              if (headArg == "--")
                stopParsing(tailArgs)
              else if (headArg.startsWith("-") && headArg != "-")
                if (ignoreUnrecognized)
                  helper(
                    current,
                    tailArgs,
                    Indexed(index, 1, headArg) :: extraArgsReverse,
                    Step.IgnoredUnrecognized(index) :: reverseSteps,
                    index + 1
                  )
                else
                  unrecognized(headArg, tailArgs)
              else if (stopAtFirstUnrecognized)
                stoppingAtUnrecognized
              else
                helper(
                  current,
                  tailArgs,
                  Indexed(index, 1, headArg) :: extraArgsReverse,
                  Step.StandardArgument(index) :: reverseSteps,
                  index + 1
                )

            case Right(Some((newC, matchedArg, newArgs))) =>
              assert(
                newArgs != args,
                s"From $args, an ArgParser is supposed to have consumed arguments, but returned the same argument list"
              )

              val consumed0 = Parser.consumed(args, newArgs)
              assert(consumed0 > 0)

              helper(
                newC,
                newArgs.toList,
                extraArgsReverse,
                Step.MatchedOption(index, consumed0, matchedArg) :: reverseSteps,
                index + consumed0
              )

            case Left((msg, matchedArg, rem)) =>
              val consumed0 = Parser.consumed(args, rem)
              assert(consumed0 > 0)
              val (remaining, remArgs, steps) = runHelper(
                current,
                rem,
                extraArgsReverse,
                Step.ErroredOption(index, consumed0, matchedArg, msg) :: reverseSteps,
                index + consumed0
              )
              val res = Left((
                remaining.fold(errs => msg.append(errs._1), _ => msg),
                remaining.fold(_._2, Right(_))
              ))
              (res, remArgs, steps)
          }
      }
    }

    helper(init, args.toList, Nil, Nil, 0)
  }

  /** Computes completions for the argument at index `index` in `args`
    *
    * Finds out whether this argument is an option name, an option value, or a non-option argument,
    * and calls the corresponding method of `completer`.
    *
    * @param args
    *   arguments (without the program name)
    * @param index
    *   index of the argument to complete (can be `args.length` or more, to complete a new argument)
    * @param completer
    *   computes completions for option names, option values, and arguments
    */
  def complete(
    args: Seq[String],
    index: Int,
    completer: Completer[T],
    stopAtFirstUnrecognized: Boolean,
    ignoreUnrecognized: Boolean
  ): List[CompletionItem] = {

    val args0 = if (index < args.length) args else args ++ Seq.fill(index + 1 - args.length)("")

    val (res, remArgs, steps) = scan(args0, stopAtFirstUnrecognized, ignoreUnrecognized)
    lazy val stateOpt = res match {
      case Left((_, Left(state))) => get(state).toOption
      case Left((_, Right(t)))    => Some(t)
      case Right(t)               => Some(t)
    }

    assert(index >= 0)
    assert(index < args0.length)

    val prefix = args0(index)

    val stepOpt = steps.find { step =>
      step.index <= index && index < step.index + step.consumed
    }

    val value = args0(index)

    stepOpt match {
      case None =>
        val isAfterDoubleDash = steps.lastOption.exists {
          case Step.DoubleDash(ddIdx) => ddIdx < index
          case _                      => false
        }
        if (isAfterDoubleDash)
          completer.postDoubleDash(stateOpt, remArgs)
            .map { completer =>
              if (value.startsWith("-"))
                completer.optionName(value, stateOpt, remArgs)
              else
                completer.argument(value, stateOpt, remArgs)
            }
            .getOrElse(Nil)
        else
          Nil
      case Some(step) =>
        val shift = index - step.index
        step match {
          case Step.DoubleDash(_) =>
            completer.optionName(value, stateOpt, remArgs)
          case Step.ErroredOption(_, _, _, _) if shift == 0 =>
            completer.optionName(value, stateOpt, remArgs)
          case Step.ErroredOption(_, consumed, arg, _) if consumed == 2 && shift == 1 =>
            completer.optionValue(arg, value, stateOpt, remArgs)
          case Step.ErroredOption(_, _, _, _) =>
            // should not happen
            Nil
          case Step.FirstUnrecognized(_, true) =>
            completer.optionName(value, stateOpt, remArgs)
          case Step.FirstUnrecognized(_, false) =>
            completer.argument(value, stateOpt, remArgs)
          case Step.IgnoredUnrecognized(_) =>
            completer.optionName(value, stateOpt, remArgs)
          case Step.Unrecognized(_, _) =>
            completer.optionName(value, stateOpt, remArgs)
          case Step.StandardArgument(idx) if value == "-" =>
            completer.optionName(value, stateOpt, remArgs)
          case Step.MatchedOption(_, consumed, arg) if shift == 0 =>
            completer.optionName(value, stateOpt, remArgs)
          case Step.MatchedOption(_, consumed, arg) if consumed == 2 && shift == 1 =>
            completer.optionValue(arg, value, stateOpt, remArgs)
          case Step.MatchedOption(_, _, _) =>
            // should not happen
            Nil
          case Step.StandardArgument(_) =>
            completer.argument(value, stateOpt, remArgs)
        }
    }
  }

}
