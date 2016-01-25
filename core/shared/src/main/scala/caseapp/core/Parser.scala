package caseapp
package core

import shapeless._
import shapeless.compat.Annotations

import caseapp.util.AnnotationList

trait Parser[T] { self =>
  type D
  def init: D
  def step(args: Seq[String], d: D): Either[String, Option[(D, Seq[String])]]
  def get(d: D): Either[String, T]

  def args: Seq[Arg]

  def parse(args: Seq[String]): Either[String, (T, Seq[String])] =
    apply(args)

  def apply(args: Seq[String]): Either[String, (T, Seq[String])] = {
    import scala.::

    def helper(
      current: D,
      args: Seq[String],
      extraArgsReverse: List[String]
    ): Either[String, (T, List[String])] =
      if (args.isEmpty)
        get(current).right.map((_, extraArgsReverse.reverse))
      else
        step(args, current) match {
          case Right(None) =>
            args match {
              case "--" :: t =>
                helper(current, Nil, (extraArgsReverse /: t)(_.::(_)))
              case opt :: _ if opt startsWith "-" =>
                Left(s"Unrecognized argument: $opt")
              case userArg :: rem =>
                helper(current, rem, userArg :: extraArgsReverse)
            }

          case Right(Some((newC, newArgs))) =>
            assert(newArgs != args)
            helper(newC, newArgs, extraArgsReverse)

          case Left(msg) =>
            Left(msg)
        }

    helper(init, args.toList, Nil)
  }

  def withHelp: Parser[WithHelp[T]] = {
    implicit val parser: Parser.Aux[T, D] = this
    Parser[WithHelp[T]]
  }

  def map[U](f: T => U): Parser.Aux[U, D] =
    new Parser[U] {
      type D = self.D
      def init = self.init
      def step(args: Seq[String], d: D) = self.step(args, d)
      def get(d: D) = self.get(d).right.map(f)
      def args = self.args
    }
}

object Parser {
  def apply[T](implicit parser: Parser[T]): Aux[T, parser.D] = parser

  type Aux[T, D0] = Parser[T] { type D = D0 }

  implicit def generic[CC, L <: HList, D <: HList, N <: HList, V <: HList, M <: HList, H <: HList, R <: HList, P <: HList]
   (implicit
    gen: LabelledGeneric.Aux[CC, L],
    defaults: shapeless.compat.Default.AsOptions.Aux[CC, D],
    names: AnnotationList.Aux[Name, CC, N],
    valuesDesc: Annotations.Aux[ValueDescription, CC, V],
    helpMessages: Annotations.Aux[HelpMessage, CC, M],
    noHelp: Annotations.Aux[Hidden, CC, H],
    recurse: Annotations.Aux[Recurse, CC, R],
    parser: Lazy[HListParser.Aux[L, D, N, V, M, H, R, P]]
   ): Aux[CC, P] =
    parser.value(defaults(), names(), valuesDesc(), helpMessages(), noHelp()).map(gen.from)
}

