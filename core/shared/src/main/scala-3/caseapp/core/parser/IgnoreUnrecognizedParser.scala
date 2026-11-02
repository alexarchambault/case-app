package caseapp.core.parser

import caseapp.core.{Arg, Error}
import caseapp.core.util.Formatter
import caseapp.Name

case class IgnoreUnrecognizedParser[T](underlying: Parser[T]) extends Parser[T] {
  import IgnoreUnrecognizedParser._
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
  def args: Seq[Arg] =
    underlying.args
  override def defaultStopAtFirstUnrecognized: Boolean =
    underlying.defaultStopAtFirstUnrecognized
  override def defaultIgnoreUnrecognized: Boolean =
    true
  override def defaultNameFormatter: Formatter[Name] =
    underlying.defaultNameFormatter

  def withDefaultOrigin(origin: String): Parser[T] =
    this.copy(underlying = underlying.withDefaultOrigin(origin))
}

object IgnoreUnrecognizedParser {

  implicit class IgnoreUnrecognizedParserWithOps[T](
    private val parser: IgnoreUnrecognizedParser[T]
  ) extends AnyVal {
    @deprecated("Use copy instead", "2.1.2")
    def withUnderlying(underlying: Parser[T]): IgnoreUnrecognizedParser[T] =
      parser.copy(underlying = underlying)
  }

}
