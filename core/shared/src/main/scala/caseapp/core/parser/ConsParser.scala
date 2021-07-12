package caseapp.core.parser

import caseapp.core.argparser.ArgParser
import caseapp.core.{Arg, Error}
import caseapp.core.util.NameOps.toNameOps
import shapeless.{:: => :*:, HList}
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

@data class ConsParser[H, T <: HList, DT <: HList](
  arg: Arg,
  argParser: ArgParser[H],
  default: () => Option[H], // FIXME Couldn't this be Option[() => H]?
  tail: Parser.Aux[T, DT]
) extends Parser[H :*: T] {

  private val argument = Argument(arg, argParser, default)

  type D = Option[H] :*: DT

  def init: D =
    argument.init :: tail.init

  def step(
      args: List[String],
      d: Option[H] :*: tail.D,
      nameFormatter: Formatter[Name]
  ): Either[(Error, Arg, List[String]), Option[(D, Arg, List[String])]] =
    argument.step(args, d.head, nameFormatter) match {
      case Left((err, rem)) => Left((err, arg, rem))
      case Right(Some((dHead, rem))) =>
        Right(Some((dHead :: d.tail, arg, rem)))
      case Right(None) =>
        tail
          .step(args, d.tail, nameFormatter)
          .map(_.map {
            case (t, arg, args) => (d.head :: t, arg, args)
          })
    }

  def get(d: D, nameFormatter: Formatter[Name]): Either[Error, H :*: T] = {

    val maybeHead = argument.get(d.head, nameFormatter)
    val maybeTail = tail.get(d.tail)

    (maybeHead, maybeTail) match {
      case (Left(headErr), Left(tailErrs)) => Left(headErr.append(tailErrs))
      case (Left(headErr), _) => Left(headErr)
      case (_, Left(tailErrs)) => Left(tailErrs)
      case (Right(h), Right(t)) => Right(h :: t)
    }
  }

  val args: Seq[Arg] =
    arg +: tail.args

  def mapHead[I](f: H => I): Parser.Aux[I :*: T, D] =
    map { l =>
      f(l.head) :: l.tail
    }

  def ::[A](argument: Argument[A]): ConsParser[A, H :*: T, D] =
    ConsParser[A, H :*: T, D](
      argument.arg,
      argument.argParser,
      argument.default,
      this
    )

  def withDefaultOrigin(origin: String): Parser.Aux[H :*: T, D] =
    withArg(arg.withDefaultOrigin(origin))
      .withTail(tail.withDefaultOrigin(origin))
}
