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

  type D = Option[H] :*: DT

  def init: D =
    None :: tail.init

  def step(args: List[String], d: Option[H] :*: tail.D): Either[Error, Option[(D, List[String])]] =
    args match {
      case Nil =>
        Right(None)

      case firstArg :: rem =>
        val matchedOpt = (Iterator(arg.name) ++ arg.extraNames.iterator)
          .map(n => n -> n(firstArg))
          .collectFirst {
            case (n, Right(valueOpt)) => n -> valueOpt
          }

        matchedOpt match {
          case Some((name, valueOpt)) =>

            val res = valueOpt match {
              case Some(value) =>
                argParser(d.head, value)
                  .right
                  .map { h =>
                    Some((Some(h) :: d.tail, rem))
                  }
              case None =>
                rem match {
                  case Nil =>
                    argParser(d.head)
                      .right
                      .map(h => Some((Some(h) :: d.tail, Nil)))
                  case th :: tRem =>
                    argParser
                      .optional(d.head, th)
                      .right
                      .map {
                        case (Consumed(usedArg), h) =>
                          Some((Some(h) :: d.tail, if (usedArg) tRem else rem))
                      }
                }
            }

            res.left.map { err =>
              Error.ParsingArgument(name, err)
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

  def get(d: D): Either[Error, H :*: T] = {

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

}
