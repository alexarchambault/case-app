package caseapp.core.parser

import caseapp.core.Scala3Helpers._
import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

case class EitherParser[T](underlying: Parser[T])
    extends Parser[Either[Error, T]] {

  import EitherParser._

  type D = underlying.D

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

  def withDefaultOrigin(origin: String): Parser[Either[Error, T]] =
    this.withUnderlying(underlying.withDefaultOrigin(origin))
}

object EitherParser {

  implicit class EitherParserWithOps[T](private val parser: EitherParser[T])
      extends AnyVal {
    def withUnderlying(underlying: Parser[T]): EitherParser[T] =
      parser.copy(underlying = underlying)
  }

}
