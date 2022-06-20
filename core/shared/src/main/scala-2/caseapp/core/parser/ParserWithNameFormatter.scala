package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

@data class ParserWithNameFormatter[T, D0](underlying: Parser.Aux[T, D0], f: Formatter[Name])
    extends Parser[T] {
  type D = D0

  def init: D = underlying.init

  def step(
    args: List[String],
    index: Int,
    d: D,
    nameFormatter: Formatter[Name]
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
    underlying.step(args, index, d, nameFormatter)

  def get(d: D, nameFormatter: Formatter[Name]): Either[Error, T] =
    underlying.get(d, nameFormatter)

  def args: Seq[Arg] = underlying.args

  override def defaultStopAtFirstUnrecognized: Boolean =
    underlying.defaultStopAtFirstUnrecognized

  override def defaultIgnoreUnrecognized: Boolean =
    underlying.defaultIgnoreUnrecognized

  override def defaultNameFormatter: Formatter[Name] = f

  def withDefaultOrigin(origin: String): Parser.Aux[T, D] =
    withUnderlying(underlying.withDefaultOrigin(origin))
}
