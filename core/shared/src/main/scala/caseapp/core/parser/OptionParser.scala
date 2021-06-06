package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

@data class OptionParser[T, D0](underlying: Parser.Aux[T, D0]) extends Parser[Option[T]] {

  type D = D0

  def init: D =
    underlying.init

  def step(
      args: List[String],
      d: D,
      nameFormatter: Formatter[Name]
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
    underlying.step(args, d, nameFormatter)

  def get(d: D, nameFormatter: Formatter[Name]): Right[Error, Option[T]] =
    Right(underlying.get(d, nameFormatter).toOption)

  def args: Seq[Arg] =
    underlying.args

  override def defaultStopAtFirstUnrecognized: Boolean =
    underlying.defaultStopAtFirstUnrecognized

  override def defaultIgnoreUnrecognized: Boolean =
    underlying.defaultIgnoreUnrecognized

  override def defaultNameFormatter: Formatter[Name] =
    underlying.defaultNameFormatter
}
