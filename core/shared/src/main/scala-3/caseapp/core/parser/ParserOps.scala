package caseapp.core.parser

import caseapp.{HelpMessage, Name, ValueDescription}
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

  // def addAll[U]: ParserOps.AddAllHelper[T, D, U] =
  //   new ParserOps.AddAllHelper[T, D, U](parser)

  def as[F](implicit helper: ParserOps.AsHelper[T, F]): Parser[F] =
    helper(parser)

  def tupled: Parser[ParserOps.Reverse[T]] =
    parser.map(ParserOps.reverse)

  def to[F](implicit helper: ParserOps.ToHelper[T, F]): Parser[F] =
    helper(parser)

  def toTuple[P](implicit helper: ParserOps.ToTupleHelper[T, P]): Parser[P] =
    helper(parser)

}

object ParserOps {

  type Reverse[T <: Tuple] <: Tuple = T match {
    case EmptyTuple => EmptyTuple
    case x *: xs    => Tuple.Concat[Reverse[xs], x *: EmptyTuple]
  }

  def reverse[T <: Tuple](t: T): Reverse[T] =
    Tuple.fromArray(t.toArray.reverse).asInstanceOf[Reverse[T]]

  def reverse0[T <: Tuple](t: Reverse[T]): T =
    Tuple.fromArray(t.toArray.reverse).asInstanceOf[T]

  // class AddAllHelper[T <: Tuple, D <: Tuple, U](val parser: Parse[T]) extends AnyVal {
  //   def apply[DU](implicit other: Parser[U]): Parser[U :: T] =
  //     RecursiveConsParser(other, parser)
  // }

  abstract class AsHelper[T, F] {
    def apply(parser: Parser[T]): Parser[F]
  }

  inline implicit def defaultAsHelper[T <: Tuple, F](implicit
    m: Mirror.ProductOf[F],
    ev: T =:= Reverse[m.MirroredElemTypes]
  ): AsHelper[T, F] = {
    parser =>
      parser.map(reverse0).map(p => m.fromProduct(p))
  }

  abstract class ToHelper[T, F] {
    def apply(parser: Parser[T]): Parser[F]
  }

  implicit def defaultToHelper[F, T <: Tuple](implicit
    m: Mirror.ProductOf[F]
  ): ToHelper[T, F] = {
    parser =>
      parser.map(m.fromProduct)
  }

  sealed abstract class ToTupleHelper[T, P] {
    def apply(parser: Parser[T]): Parser[P]
  }

  implicit def defaultToTupleHelper[T <: Product](implicit
    m: scala.deriving.Mirror.ProductOf[T]
  ): ToTupleHelper[T, m.MirroredElemTypes] =
    new ToTupleHelper[T, m.MirroredElemTypes] {
      def apply(parser: Parser[T]): Parser[m.MirroredElemTypes] =
        parser.map(Tuple.fromProductTyped[T](_))
    }

}
