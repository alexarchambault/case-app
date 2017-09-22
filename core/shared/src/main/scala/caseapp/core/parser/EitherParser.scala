package caseapp.core.parser

import caseapp.core.{Arg, Error}

final case class EitherParser[T, D0](underlying: Parser.Aux[T, D0]) extends Parser[Either[Error, T]] {

  type D = D0

  def init = underlying.init

  def step(args: List[String], d: D): Either[Error, Option[(D, List[String])]] =
    underlying.step(args, d)

  def get(d: D): Right[Error, Either[Error, T]] =
    Right(underlying.get(d))

  def args: Seq[Arg] =
    underlying.args

}
