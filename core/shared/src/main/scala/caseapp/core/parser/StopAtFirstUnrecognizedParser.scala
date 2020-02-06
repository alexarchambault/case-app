package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data

@data class StopAtFirstUnrecognizedParser[T](underlying: Parser[T]) extends Parser[T] {
  type D = underlying.D
  def init: D = underlying.init
  def step(args: List[String], d: D): Either[(Error, List[String]), Option[(D, List[String])]] =
    underlying.step(args, d)
  def get(d: D): Either[Error, T] =
    underlying.get(d)
  def args: Seq[Arg] =
    underlying.args
  override def defaultStopAtFirstUnrecognized: Boolean =
    true
}
