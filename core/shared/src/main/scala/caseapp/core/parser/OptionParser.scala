package caseapp.core.parser

import caseapp.core.{Arg, Error}

final case class OptionParser[T, D0](underlying: Parser.Aux[T, D0]) extends Parser[Option[T]] {

  override type D = D0

  override def init: D =
    underlying.init

  override def step(args: List[String], d: D): Either[Error, Option[(D, List[String])]] =
    underlying.step(args, d)

  override def get(d: D): Either[Seq[Error], Option[T]] =
    Right(underlying.get(d).right.toOption)

  override def args: Seq[Arg] =
    underlying.args

}
