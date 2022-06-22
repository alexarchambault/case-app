package caseapp.core.parser

import caseapp.core.Scala3Helpers._
import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

@data case class MappedParser[T, D0, U](underlying: Parser.Aux[T, D0], f: T => U)
    extends Parser[U] {

  type D = D0

  def init: D =
    underlying.init

  def step(
    args: List[String],
    index: Int,
    d: D,
    nameFormatter: Formatter[Name]
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
    underlying.step(args, index, d, nameFormatter)

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
    this.withUnderlying(underlying.withDefaultOrigin(origin))
}
