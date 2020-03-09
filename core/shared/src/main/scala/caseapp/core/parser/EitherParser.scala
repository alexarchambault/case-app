package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.OptionFormatter

@data class EitherParser[T, D0](underlying: Parser.Aux[T, D0]) extends Parser[Either[Error, T]] {

  type D = D0

  def init = underlying.init

  def step(
      args: List[String],
      d: D,
      optionFormatter: OptionFormatter
  ): Either[(Error, List[String]), Option[(D, List[String])]] =
    underlying.step(args, d, optionFormatter)

  def get(d: D, optionFormatter: OptionFormatter): Right[Error, Either[Error, T]] =
    Right(underlying.get(d, optionFormatter))

  def args: Seq[Arg] =
    underlying.args

  override def defaultStopAtFirstUnrecognized: Boolean =
    underlying.defaultStopAtFirstUnrecognized

}
