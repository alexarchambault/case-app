package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

@data class StopAtFirstUnrecognizedParser[T](underlying: Parser[T]) extends Parser[T] {
  type D = underlying.D
  def init: D = underlying.init
  def step(
      args: List[String],
      d: D,
      nameFormatter: Formatter[Name]
  ): Either[(Error, List[String]), Option[(D, List[String])]] =
    underlying.step(args, d, nameFormatter)
  def get(d: D, nameFormatter: Formatter[Name]): Either[Error, T] =
    underlying.get(d, nameFormatter)
  def args: Seq[Arg] =
    underlying.args
  override def defaultStopAtFirstUnrecognized: Boolean =
    true
  override def defaultNameFormatter: Formatter[Name] =
    underlying.defaultNameFormatter
}
