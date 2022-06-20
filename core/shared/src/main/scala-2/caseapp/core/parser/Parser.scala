package caseapp.core.parser

import caseapp.core.Error
import caseapp.core.help.{WithFullHelp, WithHelp}
import shapeless.{HList, HNil}
import caseapp.core.util.Formatter
import caseapp.Name

import scala.language.implicitConversions

/** Parses arguments, resulting in a `T` in case of success.
  *
  * @tparam T:
  *   success result type
  */
abstract class Parser[T] extends ParserMethods[T] {

  import Parser.Step

  /** Intermediate result type.
    *
    * Used during parsing, while checking the arguments one after the other.
    *
    * If parsing succeeds, a `T` can be built from the [[D]] at the end of parsing.
    */
  type D

  def stopAtFirstUnrecognized: Parser.Aux[T, D] =
    StopAtFirstUnrecognizedParser(this)

  def ignoreUnrecognized: Parser.Aux[T, D] =
    IgnoreUnrecognizedParser(this)

  def nameFormatter(f: Formatter[Name]): Parser.Aux[T, D] =
    ParserWithNameFormatter(this, f)

  /** Creates a [[Parser]] accepting help / usage arguments, out of this one.
    */
  final def withHelp: Parser[WithHelp[T]] = {
    implicit val parser: Parser.Aux[T, D] = this
    val p = ParserWithNameFormatter(Parser[WithHelp[T]], defaultNameFormatter)
    if (defaultIgnoreUnrecognized)
      p.ignoreUnrecognized
    else if (defaultStopAtFirstUnrecognized)
      p.stopAtFirstUnrecognized
    else
      p
  }

  final def withFullHelp: Parser[WithFullHelp[T]] = {
    implicit val parser: Parser.Aux[T, D] = this
    val p = ParserWithNameFormatter(Parser[WithFullHelp[T]], defaultNameFormatter)
    if (defaultIgnoreUnrecognized)
      p.ignoreUnrecognized
    else if (defaultStopAtFirstUnrecognized)
      p.stopAtFirstUnrecognized
    else
      p
  }

  final def map[U](f: T => U): Parser.Aux[U, D] =
    MappedParser(this, f)

  def withDefaultOrigin(origin: String): Parser.Aux[T, D]
}

object Parser extends ParserCompanion with LowPriorityParserImplicits {

  /** Look for an implicit `Parser[T]` */
  def apply[T](implicit parser: Parser[T]): Aux[T, parser.D] = parser

  type Aux[T, D0] = Parser[T] { type D = D0 }

  /** An empty [[Parser]].
    *
    * Can be made non empty by successively calling `add` on it.
    */
  def nil: Parser.Aux[HNil, HNil] =
    NilParser

  implicit def option[T, D](implicit parser: Aux[T, D]): Parser.Aux[Option[T], D] =
    OptionParser(parser)

  implicit def either[T, D](implicit parser: Aux[T, D]): Parser.Aux[Either[Error, T], D] =
    EitherParser(parser)

  implicit def toParserOps[T <: HList, D <: HList](parser: Aux[T, D]): ParserOps[T, D] =
    new ParserOps(parser)

}
