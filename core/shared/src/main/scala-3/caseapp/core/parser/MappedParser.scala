package caseapp.core.parser

import caseapp.core.Scala3Helpers._
import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

case class MappedParser[T, U](underlying: Parser[T], f: T => U)
    extends Parser[U] {

  import MappedParser._

  type D = underlying.D

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

  def withDefaultOrigin(origin: String): Parser[U] =
    this.withUnderlying(underlying.withDefaultOrigin(origin))
}

object MappedParser {

  implicit class MappedParserWithOps[T, U](private val parser: MappedParser[T, U])
      extends AnyVal {
    def withUnderlying(underlying: Parser[T]): MappedParser[T, U] =
      parser.copy(underlying = underlying)
  }

}
