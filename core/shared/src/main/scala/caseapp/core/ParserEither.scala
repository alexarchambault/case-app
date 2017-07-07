package caseapp.core


final case class ParserEither[T, D0](underlying: Parser.Aux[T, D0]) extends Parser[Either[String, T]] {
  type D = D0
  def init = underlying.init
  def step(args: Seq[String], d: D): Either[String, Option[(D, Seq[String])]] =
    underlying.step(args, d)
  def get(d: D): Right[String, Either[String, T]] =
    Right(underlying.get(d))

  def args: Seq[Arg] =
    underlying.args
}

