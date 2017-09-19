package caseapp.core.parser

import caseapp.core.Error
import shapeless.{::, HList}

final case class RecursiveConsParser[H, HD, T <: HList, TD <: HList](
  headParser: Parser.Aux[H, HD],
  tailParser: Parser.Aux[T, TD]
) extends Parser[H :: T] {

  override type D = HD :: TD

  override def init: D =
    headParser.init :: tailParser.init

  override def step(args: List[String], d: D): Either[Error, Option[(D, List[String])]] =
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

  override def get(d: D): Either[Seq[Error], H :: T] = {
    val maybeHead = headParser.get(d.head)
    val maybeTail = tailParser.get(d.tail)

    (maybeHead, maybeTail) match {
      case (Left(headErrs), Left(tailErrs)) => Left(headErrs ++ tailErrs)
      case (Left(headErrs), _) => Left(headErrs)
      case (_, Left(tailErrs)) => Left(tailErrs)
      case (Right(h), Right(t)) => Right(h :: t)
    }
  }

  override val args = headParser.args ++ tailParser.args

  def mapHead[I](f: H => I): Parser.Aux[I :: T, D] =
    map { l =>
      f(l.head) :: l.tail
    }

}
