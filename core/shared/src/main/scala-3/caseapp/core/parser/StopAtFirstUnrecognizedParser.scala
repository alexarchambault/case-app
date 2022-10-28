package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

case class StopAtFirstUnrecognizedParser[T](underlying: Parser[T])
    extends Parser[T] {
  import StopAtFirstUnrecognizedParser._
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
    true
  override def defaultIgnoreUnrecognized: Boolean =
    underlying.defaultIgnoreUnrecognized
  override def defaultNameFormatter: Formatter[Name] =
    underlying.defaultNameFormatter

  def withDefaultOrigin(origin: String): Parser[T] =
    this.withUnderlying(underlying.withDefaultOrigin(origin))
}

object StopAtFirstUnrecognizedParser {

  implicit class StopAtFirstUnrecognizedParserWithOps[T](
    private val parser: StopAtFirstUnrecognizedParser[T]
  ) extends AnyVal {
    def withUnderlying(underlying: Parser[T]): StopAtFirstUnrecognizedParser[T] =
      parser.copy(underlying = underlying)
  }

}
