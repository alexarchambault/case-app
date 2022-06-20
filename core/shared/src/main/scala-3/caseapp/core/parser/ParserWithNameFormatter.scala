package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

case class ParserWithNameFormatter[T](underlying: Parser[T], f: Formatter[Name])
    extends Parser[T] {
  import ParserWithNameFormatter._
  type D = underlying.D

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

  def withDefaultOrigin(origin: String): Parser[T] =
    this.withUnderlying(underlying.withDefaultOrigin(origin))
}

object ParserWithNameFormatter {

  implicit class ParserWithNameFormatterWithOps[T](private val parser: ParserWithNameFormatter[
    T
  ]) extends AnyVal {
    def withUnderlying(underlying: Parser[T]): ParserWithNameFormatter[T] =
      parser.copy(underlying = underlying)
  }

}
