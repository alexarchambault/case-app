package caseapp.core.parser

import caseapp.core.Error
import shapeless.{::, HList}

final case class RecursiveConsParser[H, HD, T <: HList, TD <: HList](
  headParser: Parser.Aux[H, HD],
  tailParser: Parser.Aux[T, TD]
) extends Parser[H :: T] {

  type D = HD :: TD

  def init: D =
    headParser.init :: tailParser.init

  def step(args: List[String], d: D): Either[Error, Option[(D, List[String])]] =
    headParser
      .step(args, d.head)
      .right
      .flatMap {
        case None =>
          tailParser
            .step(args, d.tail)
            .right
            .map(_.map {
              case (t, args) => (d.head :: t, args)
            })
        case Some((h, args)) =>
          Right(Some(h :: d.tail, args))
      }

  def get(d: D): Either[Error, H :: T] =
    for {
      h <- headParser.get(d.head).right
      t <- tailParser.get(d.tail).right
    } yield h :: t

  val args = headParser.args ++ tailParser.args

  def mapHead[I](f: H => I): Parser.Aux[I :: T, D] =
    map { l =>
      f(l.head) :: l.tail
    }

}
