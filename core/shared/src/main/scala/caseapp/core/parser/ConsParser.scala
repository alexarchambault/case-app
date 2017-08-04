package caseapp.core.parser

import caseapp.Name
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
        val matchedOpt = (Iterator(Name(arg.name)) ++ arg.extraNames.iterator)
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

  def get(d: D): Either[Error, H :*: T] = {

    val maybeHead = d.head
      .orElse(default)
      .toRight {
        def prefixed(name: String) =
          if (name.length == 1)
            "-" + name
          else
            "--" + name

        Error.RequiredOptionNotSpecified(
          prefixed(arg.name),
          arg.extraNames.map(_.name).filter(_ != arg.name).map(prefixed)
        )
      }

    for {
      h <- maybeHead.right
      t <- tail.get(d.tail).right
    } yield h :: t
  }

  val args: Seq[Arg] =
    arg +: tail.args

  def mapHead[I](f: H => I): Parser.Aux[I :*: T, D] =
    map { l =>
      f(l.head) :: l.tail
    }

}
