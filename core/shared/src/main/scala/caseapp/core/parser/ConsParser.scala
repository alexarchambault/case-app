package caseapp.core.parser

import caseapp.core.argparser.{ArgParser, Consumed}
import caseapp.core.{Arg, Error}
import caseapp.core.util.NameOps.toNameOps
import shapeless.{:: => :*:, HList}

final case class ConsParser[H, T <: HList, DT <: HList](
  arg: Arg,
  argParser: ArgParser[H],
  default: Option[H],
  tail: Parser.Aux[T, DT]
) extends Parser[H :*: T] {

  override type D = Option[H] :*: DT

  override def init: D =
    None :: tail.init

  override def step(args: List[String], d: Option[H] :*: tail.D): Either[Error, Option[(D, List[String])]] =
    args match {
      case Nil =>
        Right(None)

      case firstArg :: rem =>
        val matchedOpt = (Iterator(arg.name) ++ arg.extraNames.iterator)
          .map(_.apply(firstArg))
          .collectFirst {
            case Right(valueOpt) => valueOpt
          }

        matchedOpt match {
          case Some(Some(value)) =>
            argParser(d.head, value)
              .right
              .map { h =>
                Some((Some(h) :: d.tail, rem))
              }

          case Some(None) =>
            rem match {
              case Nil =>
                argParser(d.head)
                  .right
                  .map(h => Some((Some(h) :: d.tail, Nil)))
              case th :: tRem =>
                argParser.optional(d.head, th)
                  .right
                  .map {
                    case (Consumed(usedArg), h) =>
                      Some((Some(h) :: d.tail, if (usedArg) tRem else rem))
                  }
            }

          case None =>
            tail
              .step(args, d.tail)
              .right
              .map(_.map {
                case (t, args) => (d.head :: t, args)
              })
        }
    }

  override def get(d: D): Either[Seq[Error], H :*: T] = {

    val maybeHead = d.head
      .orElse(default)
      .toRight {
        Error.RequiredOptionNotSpecified(
          arg.name.option,
          arg.extraNames.map(_.option)
        )
      }

    val maybeTail = tail.get(d.tail)

    (maybeHead, maybeTail) match {
      case (Left(headErr), Left(tailErrs)) => Left(headErr +: tailErrs)
      case (Left(headErr), _) => Left(Seq(headErr))
      case (_, Left(tailErrs)) => Left(tailErrs)
      case (Right(h), Right(t)) => Right(h :: t)
    }
  }

  override val args: Seq[Arg] =
    arg +: tail.args

  def mapHead[I](f: H => I): Parser.Aux[I :*: T, D] =
    map { l =>
      f(l.head) :: l.tail
    }

}
