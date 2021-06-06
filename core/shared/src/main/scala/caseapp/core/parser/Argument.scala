package caseapp.core.parser

import caseapp.core.argparser.{ArgParser, Consumed}
import caseapp.core.{Arg, Error}
import caseapp.core.util.NameOps.toNameOps
import dataclass.data
import caseapp.core.util.Formatter
import caseapp.Name

@data class Argument[H](
  arg: Arg,
  argParser: ArgParser[H],
  default: () => Option[H]
) {

  def init: Option[H] =
    None

  def step(
      args: List[String],
      d: Option[H],
      nameFormatter: Formatter[Name]
  ): Either[(Error, List[String]), Option[(Option[H], List[String])]] =
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
                val res0 = argParser(d, value)
                  .map(h => Some(Some(h)))
                (res0, rem)
              case None =>
                rem match {
                  case Nil =>
                    val res0 = argParser(d)
                      .map(h => Some(Some(h)))
                    (res0, Nil)
                  case th :: tRem =>
                    val (Consumed(usedArg), res) = argParser.optional(d, th)
                    val res0 = res.map(h => Some(Some(h)))
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
            Right(None)
        }
    }

  def get(d: Option[H], nameFormatter: Formatter[Name]): Either[Error, H] =
    d.orElse(default()).toRight {
      Error.RequiredOptionNotSpecified(
        arg.name.option(nameFormatter),
        arg.extraNames.map(_.option(nameFormatter))
      )
    }

  val args: Seq[Arg] =
    Seq(arg)

}

object Argument {
  def apply[H: ArgParser](arg: Arg): Argument[H] =
    Argument[H](arg, ArgParser[H], () => None)
}
