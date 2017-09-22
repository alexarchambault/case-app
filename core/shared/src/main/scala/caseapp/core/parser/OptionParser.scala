package caseapp.core.parser

import caseapp.core.{Arg, Error}

final case class OptionParser[T, D0](underlying: Parser.Aux[T, D0]) extends Parser[Option[T]] {

  type D = D0

  def init: D =
    underlying.init

  def step(args: List[String], d: D): Either[Error, Option[(D, List[String])]] =
    underlying.step(args, d)

  def get(d: D): Right[Error, Option[T]] =
    Right(underlying.get(d).right.toOption)

  def args: Seq[Arg] =
    underlying.args

}
