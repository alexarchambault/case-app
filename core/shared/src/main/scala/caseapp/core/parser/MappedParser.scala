package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

@data class MappedParser[T, D0, U](underlying: Parser.Aux[T, D0], f: T => U) extends Parser[U] {

  type D = D0

  def init: D =
    underlying.init

  def step(
    args: List[String],
    d: D,
    nameFormatter: Formatter[Name]
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
    underlying.step(args, d, nameFormatter)

  def get(d: D, nameFormatter: Formatter[Name]): Either[Error, U] =
    underlying
      .get(d, nameFormatter)
      .map(f)

  def args: Seq[Arg] =
    underlying.args

  override def defaultStopAtFirstUnrecognized: Boolean =
    underlying.defaultStopAtFirstUnrecognized

  override def defaultIgnoreUnrecognized: Boolean =
    underlying.defaultIgnoreUnrecognized

  override def defaultNameFormatter: Formatter[Name] =
    underlying.defaultNameFormatter

  def withDefaultOrigin(origin: String): Parser.Aux[U, D] =
    withUnderlying(underlying.withDefaultOrigin(origin))
}
