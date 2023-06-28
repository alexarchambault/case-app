package caseapp.core.parser

import caseapp.{Name, Recurse}
import caseapp.core.{Arg, Error}
import caseapp.core.util.Formatter
import caseapp.core.Scala3Helpers._
import dataclass.data

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
      .step(args, index, runtime.Tuples(d, 0).asInstanceOf[headParser.D], Formatter.addRecursePrefix(recurse, nameFormatter))
      .flatMap {
        case None =>
          tailParser
            .step(args, index, runtime.Tuples.tail(d).asInstanceOf[tailParser.D], nameFormatter)
            .map(_.map {
              case (t, arg, args) =>
                (runtime.Tuples(d, 0).asInstanceOf[headParser.D] *: t, arg, args)
            })
        case Some((h, arg, args)) =>
          Right(Some(h *: runtime.Tuples.tail(d).asInstanceOf[tailParser.D], arg, args))
      }

  def get(d: D, nameFormatter: Formatter[Name]): Either[Error, H *: T] = {
    val maybeHead = headParser.get(runtime.Tuples(d, 0).asInstanceOf[headParser.D], nameFormatter)
    val maybeTail = tailParser.get(runtime.Tuples.tail(d).asInstanceOf[tailParser.D], nameFormatter)

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
      f(l.head) *: runtime.Tuples.tail(l).asInstanceOf[T]
    }

  def ::[A](argument: Argument[A]): ConsParser[A, H *: T] =
    ConsParser[A, H *: T](argument, this)

  def withDefaultOrigin(origin: String): Parser[H *: T] =
    this.withHeadParser(headParser.withDefaultOrigin(origin))
      .withTailParser(tailParser.withDefaultOrigin(origin))

}
