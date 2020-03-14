package caseapp.core.parser

import caseapp.core.Error
import shapeless.{::, HList}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

@data class RecursiveConsParser[H, HD, T <: HList, TD <: HList](
  headParser: Parser.Aux[H, HD],
  tailParser: Parser.Aux[T, TD]
) extends Parser[H :: T] {

  type D = HD :: TD

  def init: D =
    headParser.init :: tailParser.init

  def step(
      args: List[String],
      d: D,
      nameFormatter: Formatter[Name]
  ): Either[(Error, List[String]), Option[(D, List[String])]] =
    headParser
      .step(args, d.head, nameFormatter)
      .flatMap {
        case None =>
          tailParser
            .step(args, d.tail, nameFormatter)
            .map(_.map {
              case (t, args) => (d.head :: t, args)
            })
        case Some((h, args)) =>
          Right(Some(h :: d.tail, args))
      }

  def get(d: D, nameFormatter: Formatter[Name]): Either[Error, H :: T] = {
    val maybeHead = headParser.get(d.head, nameFormatter)
    val maybeTail = tailParser.get(d.tail, nameFormatter)

    (maybeHead, maybeTail) match {
      case (Left(headErrs), Left(tailErrs)) => Left(headErrs.append(tailErrs))
      case (Left(headErrs), _) => Left(headErrs)
      case (_, Left(tailErrs)) => Left(tailErrs)
      case (Right(h), Right(t)) => Right(h :: t)
    }
  }

  val args = headParser.args ++ tailParser.args

  def mapHead[I](f: H => I): Parser.Aux[I :: T, D] =
    map { l =>
      f(l.head) :: l.tail
    }

}
