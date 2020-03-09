package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.OptionFormatter

@data class MappedParser[T, D0, U](underlying: Parser.Aux[T, D0], f: T => U) extends Parser[U] {

  type D = D0

  def init: D =
    underlying.init

  def step(
      args: List[String],
      d: D,
      optionFormatter: OptionFormatter
  ): Either[(Error, List[String]), Option[(D, List[String])]] =
    underlying.step(args, d, optionFormatter)

  def get(d: D, optionFormatter: OptionFormatter): Either[Error, U] =
    underlying
      .get(d, optionFormatter)
      .map(f)

  def args: Seq[Arg] =
    underlying.args

  override def defaultStopAtFirstUnrecognized: Boolean =
    underlying.defaultStopAtFirstUnrecognized

}
