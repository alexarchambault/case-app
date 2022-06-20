package caseapp.core.parser

import caseapp.core.argparser.ArgParser
import caseapp.core.Scala3Helpers._
import caseapp.core.{Arg, Error}
import caseapp.core.util.NameOps.toNameOps
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

import scala.compiletime._
import scala.compiletime.ops._

case class ConsParser[H, T <: Tuple](
  argument: Argument[H],
  val tail: Parser[T]
) extends Parser[H *: T] {

  type D = Option[H] *: tail.D

  def init: D =
    argument.init *: tail.init

  def step(
    args: List[String],
    index: Int,
    d: D,
    nameFormatter: Formatter[Name]
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
    argument.step(args, index, runtime.Tuples(d, 0).asInstanceOf[Option[H]], nameFormatter) match {
      case Left((err, rem)) => Left((err, argument.arg, rem))
      case Right(Some((dHead, rem))) =>
        Right(Some((dHead *: runtime.Tuples.tail(d).asInstanceOf[tail.D], argument.arg, rem)))
      case Right(None) =>
        tail
          .step(args, index, runtime.Tuples.tail(d).asInstanceOf[tail.D], nameFormatter)
          .map(_.map {
            case (t, arg, args) => (runtime.Tuples(d, 0).asInstanceOf[Option[H]] *: t, arg, args)
          })
    }

  def get(d: D, nameFormatter: Formatter[Name]): Either[Error, H *: T] = {

    val maybeHead = argument.get(runtime.Tuples(d, 0).asInstanceOf[Option[H]], nameFormatter)
    val maybeTail = tail.get(runtime.Tuples.tail(d).asInstanceOf[tail.D])

    (maybeHead, maybeTail) match {
      case (Left(headErr), Left(tailErrs)) => Left(headErr.append(tailErrs))
      case (Left(headErr), _)              => Left(headErr)
      case (_, Left(tailErrs))             => Left(tailErrs)
      case (Right(h), Right(t))            => Right(h *: t)
    }
  }

  val args: Seq[Arg] =
    argument.arg +: tail.args

  def mapHead[I](f: H => I): Parser[I *: T] =
    map { l =>
      f(runtime.Tuples.apply(l, 0).asInstanceOf[H]) *: runtime.Tuples.tail(l).asInstanceOf[T]
    }

  def ::[A](argument: Argument[A]): ConsParser[A, H *: T] =
    ConsParser[A, H *: T](argument, this)

  def withDefaultOrigin(origin: String): Parser[H *: T] =
    this.withArgument(argument.withDefaultOrigin(origin))
      .withTail(tail.withDefaultOrigin(origin))
}
