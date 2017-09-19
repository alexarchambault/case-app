package caseapp.core.parser

import caseapp.core.{Arg, Error}

final case class MappedParser[T, D0, U](underlying: Parser.Aux[T, D0], f: T => U) extends Parser[U] {

  override type D = D0

  override def init: D =
    underlying.init

  override def step(args: List[String], d: D): Either[Error, Option[(D, List[String])]] =
    underlying.step(args, d)

  override def get(d: D): Either[Seq[Error], U] =
    underlying
      .get(d)
      .right
      .map(f)

  override def args: Seq[Arg] =
    underlying.args

}
