package caseapp.core

final case class ParserOption[T, D0](underlying: Parser.Aux[T, D0]) extends Parser[Option[T]] {
  type D = D0
  def init = underlying.init
  def step(args: Seq[String], d: D): Either[String, Option[(D, Seq[String])]] =
    underlying.step(args, d)
  def get(d: D): Right[String, Option[T]] =
    Right(underlying.get(d).right.toOption)

  def args: Seq[Arg] =
    underlying.args
}
