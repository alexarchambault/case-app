package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.OptionFormatter

@data class OptionParser[T, D0](underlying: Parser.Aux[T, D0]) extends Parser[Option[T]] {

  type D = D0

  def init: D =
    underlying.init

  def step(
      args: List[String],
      d: D,
      optionFormatter: OptionFormatter
  ): Either[(Error, List[String]), Option[(D, List[String])]] =
    underlying.step(args, d, optionFormatter)

  def get(d: D, optionFormatter: OptionFormatter): Right[Error, Option[T]] =
    Right(underlying.get(d, optionFormatter).toOption)

  def args: Seq[Arg] =
    underlying.args

  override def defaultStopAtFirstUnrecognized: Boolean =
    underlying.defaultStopAtFirstUnrecognized

}
