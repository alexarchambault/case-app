package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

@data(
  deprecatedSetters = true,
  deprecatedSettersMessage = "Use copy instead",
  deprecatedSettersSince = "2.1.2"
) class EitherParser[T, D0](underlying: Parser.Aux[T, D0]) extends Parser[Either[Error, T]] {

  type D = D0

  def init = underlying.init

  def step(
    args: List[String],
    index: Int,
    d: D,
    nameFormatter: Formatter[Name]
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
    underlying.step(args, index, d, nameFormatter)

  def get(d: D, nameFormatter: Formatter[Name]): Right[Error, Either[Error, T]] =
    Right(underlying.get(d, nameFormatter))

  def args: Seq[Arg] =
    underlying.args

  override def defaultStopAtFirstUnrecognized: Boolean =
    underlying.defaultStopAtFirstUnrecognized

  override def defaultIgnoreUnrecognized: Boolean =
    underlying.defaultIgnoreUnrecognized

  override def defaultNameFormatter: Formatter[Name] =
    underlying.defaultNameFormatter

  def withDefaultOrigin(origin: String): Parser.Aux[Either[Error, T], D] =
    copy(underlying = underlying.withDefaultOrigin(origin))
}
