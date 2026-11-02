package caseapp.core.parser

import caseapp.{Name, Recurse}
import caseapp.core.{Arg, Error}
import caseapp.core.util.Formatter

case class RecursiveConsParser[H, T <: Tuple](
  headParser: Parser[H],
  tailParser: Parser[T],
  recurse: Recurse
) extends Parser[H *: T] {

  type D = headParser.D *: tailParser.D

  def init: D =
    headParser.init *: tailParser.init

  def step(
    args: List[String],
    index: Int,
    d: D,
    nameFormatter: Formatter[Name]
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
    headParser
      .step(
        args,
        index,
        d.head,
        Formatter.addRecursePrefix(recurse, nameFormatter)
      )
      .flatMap {
        case None =>
          tailParser
            .step(args, index, d.tail, nameFormatter)
            .map(_.map {
              case (t, arg, args) => (d.head *: t, arg, args)
            })
        case Some((h, arg, args)) =>
          Right(Some(h *: d.tail, arg, args))
      }

  def get(d: D, nameFormatter: Formatter[Name]): Either[Error, H *: T] = {
    val maybeHead = headParser.get(d.head, nameFormatter)
    val maybeTail = tailParser.get(d.tail, nameFormatter)

    (maybeHead, maybeTail) match {
      case (Left(headErrs), Left(tailErrs)) => Left(headErrs.append(tailErrs))
      case (Left(headErrs), _)              => Left(headErrs)
      case (_, Left(tailErrs))              => Left(tailErrs)
      case (Right(h), Right(t))             => Right(h *: t)
    }
  }

  val args = headParser.args ++ tailParser.args

  def mapHead[I](f: H => I): Parser[I *: T] =
    map { l =>
      f(l.head) *: l.tail
    }

  def ::[A](argument: Argument[A]): ConsParser[A, H *: T] =
    ConsParser[A, H *: T](argument, this)

  def withDefaultOrigin(origin: String): Parser[H *: T] =
    this.copy(
      headParser = headParser.withDefaultOrigin(origin),
      tailParser = tailParser.withDefaultOrigin(origin)
    )
}
