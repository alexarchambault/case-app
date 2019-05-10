package caseapp.core.parser

import caseapp.{HelpMessage, Name, ValueDescription}
import caseapp.core.argparser.ArgParser
import caseapp.core.Arg
import shapeless.{::, Generic, HList, ops}

class ParserOps[T <: HList, D <: HList](val parser: Parser.Aux[T, D]) extends AnyVal {

  def add[H: ArgParser](
    name: String,
    default: => Option[H] = None,
    extraNames: Seq[Name] = Nil,
    valueDescription: Option[ValueDescription] = None,
    helpMessage: Option[HelpMessage] = None,
    noHelp: Boolean = false,
    isFlag: Boolean = false
  ): Parser.Aux[H :: T, Option[H] :: D] =
    ConsParser(
      Arg(
        Name(name),
        extraNames,
        valueDescription,
        helpMessage,
        noHelp,
        isFlag
      ),
      ArgParser[H],
      () => default,
      parser
    )

  def addAll[U]: ParserOps.AddAllHelper[T, D, U] =
    new ParserOps.AddAllHelper[T, D, U](parser)

  def as[F](implicit helper: ParserOps.AsHelper[T, F]): Parser.Aux[F, D] =
    helper(parser)

  def tupled[P](implicit helper: ParserOps.TupledHelper[T, P]): Parser.Aux[P, D] =
    helper(parser)

}

object ParserOps {

  class AddAllHelper[T <: HList, D <: HList, U](val parser: Parser.Aux[T, D]) extends AnyVal {
    def apply[DU](implicit other: Parser.Aux[U, DU]): Parser.Aux[U :: T, DU :: D] =
      RecursiveConsParser(other, parser)
  }


  sealed abstract class AsHelper[T, F] {
    def apply[D](parser: Parser.Aux[T, D]): Parser.Aux[F, D]
  }

  implicit def defaultAsHelper[F, T <: HList, R <: HList]
   (implicit
     gen: Generic.Aux[F, R],
     rev: ops.hlist.Reverse.Aux[T, R]
   ): AsHelper[T, F] =
    new AsHelper[T, F] {
      def apply[D](parser: Parser.Aux[T, D]) =
        parser
          .map(rev.apply)
          .map(gen.from)
    }


  sealed abstract class TupledHelper[T, P] {
    def apply[D](parser: Parser.Aux[T, D]): Parser.Aux[P, D]
  }

  implicit def defaultTupledHelper[P, T <: HList, R <: HList]
   (implicit
     rev: ops.hlist.Reverse.Aux[T, R],
     tupler: ops.hlist.Tupler.Aux[R, P]
   ): TupledHelper[T, P] =
    new TupledHelper[T, P] {
      def apply[D](parser: Parser.Aux[T, D]) =
        parser
          .map(rev.apply)
          .map(tupler.apply)
    }

}
