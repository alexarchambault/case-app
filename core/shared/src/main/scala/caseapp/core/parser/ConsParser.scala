package caseapp.core.parser

import caseapp.core.argparser.{ArgParser, Consumed}
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

  type D = Option[H] :*: DT

  def init: D =
    None :: tail.init

  def step(
      args: List[String],
      d: Option[H] :*: tail.D,
      nameFormatter: Formatter[Name]
  ): Either[(Error, List[String]), Option[(D, List[String])]] =
    args match {
      case Nil =>
        Right(None)

      case firstArg :: rem =>
        val matchedOpt = (Iterator(arg.name) ++ arg.extraNames.iterator)
          .map(n => n -> n(firstArg, nameFormatter))
          .collectFirst {
            case (n, Right(valueOpt)) => n -> valueOpt
          }

        matchedOpt match {
          case Some((name, valueOpt)) =>

            val (res, rem0) = valueOpt match {
              case Some(value) =>
                val res0 = argParser(d.head, value)
                  .map(h => Some(Some(h) :: d.tail))
                (res0, rem)
              case None =>
                rem match {
                  case Nil =>
                    val res0 = argParser(d.head)
                      .map(h => Some(Some(h) :: d.tail))
                    (res0, Nil)
                  case th :: tRem =>
                    val (Consumed(usedArg), res) = argParser.optional(d.head, th)
                    val res0 = res.map(h => Some(Some(h) :: d.tail))
                    (res0, if (usedArg) tRem else rem)
                }
            }

            res
              .left
              .map { err =>
                (Error.ParsingArgument(name, err, nameFormatter), rem0)
              }
              .map(_.map((_, rem0)))

          case None =>
            tail
              .step(args, d.tail, nameFormatter)
              .map(_.map {
                case (t, args) => (d.head :: t, args)
              })
        }
    }

  def get(d: D, nameFormatter: Formatter[Name]): Either[Error, H :*: T] = {

    val maybeHead = d.head
      .orElse(default())
      .toRight {
        Error.RequiredOptionNotSpecified(
          arg.name.option(nameFormatter),
          arg.extraNames.map(_.option(nameFormatter))
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
