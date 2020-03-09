package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.OptionFormatter

@data class OptionFormatterParser[T](underlying: Parser[T], f: OptionFormatter)
    extends Parser[T] {
  type D = underlying.D

  def init: D = underlying.init

  def step(
      args: List[String],
      d: D,
      optionFormatter: OptionFormatter
  ): Either[(Error, List[String]), Option[(D, List[String])]] =
    underlying.step(args, d, optionFormatter)

  def get(d: D, optionFormatter: OptionFormatter): Either[Error, T] =
    underlying.get(d, optionFormatter)

  def args: Seq[Arg] = underlying.args

  override def defaultOptionFormatter: OptionFormatter = f
}
