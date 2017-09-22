package caseapp.core.parser

import caseapp.core.{Arg, Error}

final case class MappedParser[T, D0, U](underlying: Parser.Aux[T, D0], f: T => U) extends Parser[U] {

  type D = D0

  def init: D =
    underlying.init

  def step(args: List[String], d: D): Either[Error, Option[(D, List[String])]] =
    underlying.step(args, d)

  def get(d: D): Either[Error, U] =
    underlying
      .get(d)
      .right
      .map(f)

  def args: Seq[Arg] =
    underlying.args

}
