package caseapp.core.parser

import caseapp.core.{Arg, Error}

final case class EitherParser[T, D0](underlying: Parser.Aux[T, D0]) extends Parser[Either[Seq[Error], T]] {

  override type D = D0

  override def init = underlying.init

  override def step(args: List[String], d: D): Either[Error, Option[(D, List[String])]] =
    underlying.step(args, d)

  override def get(d: D): Either[Seq[Error], Either[Seq[Error], T]] =
    Right(underlying.get(d))

  override def args: Seq[Arg] =
    underlying.args

}
