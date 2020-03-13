package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

@data class EitherParser[T, D0](underlying: Parser.Aux[T, D0]) extends Parser[Either[Error, T]] {

  type D = D0

  def init = underlying.init

  def step(
      args: List[String],
      d: D,
      nameFormatter: Formatter[Name]
  ): Either[(Error, List[String]), Option[(D, List[String])]] =
    underlying.step(args, d, nameFormatter)

  def get(d: D, nameFormatter: Formatter[Name]): Right[Error, Either[Error, T]] =
    Right(underlying.get(d, nameFormatter))

  def args: Seq[Arg] =
    underlying.args

  override def defaultStopAtFirstUnrecognized: Boolean =
    underlying.defaultStopAtFirstUnrecognized

  override def defaultNameFormatter: Formatter[Name] =
    underlying.defaultNameFormatter

}
