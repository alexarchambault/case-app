package caseapp.core.parser

import caseapp.core.{Arg, Error}
import dataclass.data

@data class MappedParser[T, D0, U](underlying: Parser.Aux[T, D0], f: T => U) extends Parser[U] {

  type D = D0

  def init: D =
    underlying.init

  def step(args: List[String], d: D): Either[(Error, List[String]), Option[(D, List[String])]] =
    underlying.step(args, d)

  def get(d: D): Either[Error, U] =
    underlying
      .get(d)
      .map(f)

  def args: Seq[Arg] =
    underlying.args

}
