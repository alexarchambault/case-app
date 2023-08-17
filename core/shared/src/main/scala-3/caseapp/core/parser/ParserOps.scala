package caseapp.core.parser

import caseapp.{HelpMessage, Name, Recurse, ValueDescription}
import caseapp.core.argparser.ArgParser
import caseapp.core.Arg
import caseapp.core.util.Formatter
import scala.deriving.Mirror

class ParserOps[T <: Tuple](val parser: Parser[T]) extends AnyVal {

  // FIXME group is missing
  def add[H: ArgParser](
    name: String,
    default: => Option[H] = None,
    extraNames: Seq[Name] = Nil,
    valueDescription: Option[ValueDescription] = None,
    helpMessage: Option[HelpMessage] = None,
    noHelp: Boolean = false,
    isFlag: Boolean = false,
    formatter: Formatter[Name] = Formatter.DefaultNameFormatter
  ): Parser[H *: T] = {
    val argument = Argument(
      Arg(
        Name(name),
        extraNames,
        valueDescription,
        helpMessage,
        noHelp,
        isFlag
      ),
      ArgParser[H],
      () => default
    )
    ConsParser(argument, parser)
  }

  def addAll[H](using headParser: Parser[H]): Parser[H *: T] =
    RecursiveConsParser(headParser, parser, Recurse())

  def as[F](using
    m: Mirror.ProductOf[F],
    ev: T =:= ParserOps.Reverse[m.MirroredElemTypes],
    ev0: ParserOps.Reverse[ParserOps.Reverse[m.MirroredElemTypes]] =:= m.MirroredElemTypes
  ): Parser[F] =
    parser
      .map(ev)
      .map(ParserOps.reverse[ParserOps.Reverse[m.MirroredElemTypes]])
      .map(ev0)
      .map(m.fromTuple)

  def tupled: Parser[ParserOps.Reverse[T]] =
    parser.map(ParserOps.reverse)

  def to[F](using
    m: Mirror.ProductOf[F],
    ev: T =:= m.MirroredElemTypes
  ): Parser[F] =
    parser.map(ev).map(m.fromTuple)

  def toTuple[P <: Tuple](using
    m: Mirror.ProductOf[T] { type MirroredElemTypes = P }
  ): Parser[P] =
    parser.map(Tuple.fromProductTyped[T])

}

object ParserOps {

  type Reverse[T <: Tuple] <: Tuple = T match {
    case EmptyTuple => EmptyTuple
    case x *: xs    => Tuple.Concat[Reverse[xs], x *: EmptyTuple]
  }

  def reverse[T <: Tuple](t: T): Reverse[T] =
    Tuple.fromArray(t.toArray.reverse).asInstanceOf[Reverse[T]]

}
