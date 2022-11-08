package caseapp.core.parser

import caseapp.core.Error
import caseapp.core.help.{WithFullHelp, WithHelp}
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
  type D <: Tuple

  def stopAtFirstUnrecognized: Parser[T] =
    StopAtFirstUnrecognizedParser(this)
  def ignoreUnrecognized: Parser[T] =
    IgnoreUnrecognizedParser(this)

  def nameFormatter(f: Formatter[Name]): Parser[T] =
    ParserWithNameFormatter(this, f)

  /** Creates a [[Parser]] accepting help / usage arguments, out of this one.
    */
  final def withHelp: Parser[WithHelp[T]] = {
    implicit val parser: Parser[T] = this
    val p = ParserWithNameFormatter(WithHelp.parser[T], defaultNameFormatter)
    if (defaultIgnoreUnrecognized)
      p.ignoreUnrecognized
    else if (defaultStopAtFirstUnrecognized)
      p.stopAtFirstUnrecognized
    else
      p
  }

  final def withFullHelp: Parser[WithFullHelp[T]] = {
    implicit val parser: Parser[T] = this
    val p0                         = WithFullHelp.parser[T]
    val p                          = ParserWithNameFormatter(p0, defaultNameFormatter)
    if (defaultIgnoreUnrecognized)
      p.ignoreUnrecognized
    else if (defaultStopAtFirstUnrecognized)
      p.stopAtFirstUnrecognized
    else
      p
  }

  final def map[U](f: T => U): Parser[U] =
    MappedParser(this, f)

  def withDefaultOrigin(origin: String): Parser[T]
}

object Parser extends ParserCompanion with LowPriorityParserImplicits {

  /** Look for an implicit `Parser[T]` */
  def apply[T](implicit parser: Parser[T]): Parser[T] = parser

  def nil: Parser[EmptyTuple] =
    NilParser

  implicit def option[T](implicit parser: Parser[T]): Parser[Option[T]] =
    OptionParser(parser)

  implicit def either[T](implicit parser: Parser[T]): Parser[Either[Error, T]] =
    EitherParser(parser)

  implicit def toParserOps[T <: Tuple](parser: Parser[T]): ParserOps[T] =
    new ParserOps(parser)

}
