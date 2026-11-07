package caseapp.core.parser

import caseapp.core.Error
import caseapp.core.help.{WithFullHelp, WithHelp}
import caseapp.core.util.Formatter
import caseapp.Name

import scala.language.implicitConversions

/** Parses arguments, resulting in a `T` in case of success.
  *
  * Parsers for case classes are derived automatically, from the types of their fields (which need
  * an implicit [[caseapp.core.argparser.ArgParser]]), their default values, and the annotations on
  * them (like `Name`, `HelpMessage`, `Recurse`…). They can also be derived explicitly, with
  * `Parser.derive`, or with a `derives Parser` clause.
  *
  * {{{
  * case class Options(
  *   foo: Int = 0,
  *   bar: Option[String] = None
  * )
  *
  * Parser[Options].parse(Seq("--foo", "2", "other"))
  * // Right((Options(2, None), Seq("other")))
  * }}}
  *
  * See [[caseapp.core.parser.ParserMethods]] for the methods actually parsing arguments, like
  * `parse` or `detailedParse`.
  *
  * @tparam T
  *   success result type
  */
abstract class Parser[+T] extends ParserMethods[T] {

  import Parser.Step

  /** Intermediate result type.
    *
    * Used during parsing, while checking the arguments one after the other.
    *
    * If parsing succeeds, a `T` can be built from the [[D]] at the end of parsing.
    */
  type D <: Tuple

  /** Creates a parser that stops parsing at the first unrecognized argument, out of this one
    *
    * Arguments after it, and the unrecognized argument itself, are left in the remaining arguments.
    * See [[caseapp.core.app.CaseApp.stopAtFirstUnrecognized]].
    */
  def stopAtFirstUnrecognized: Parser[T] =
    StopAtFirstUnrecognizedParser(this)

  /** Creates a parser that ignores unrecognized options, out of this one
    *
    * Unrecognized options are left in the remaining arguments, rather than making parsing fail. See
    * [[caseapp.core.app.CaseApp.ignoreUnrecognized]].
    */
  def ignoreUnrecognized: Parser[T] =
    IgnoreUnrecognizedParser(this)

  /** Creates a parser that formats field names into option names with `f`, out of this one
    *
    * The default formatter, [[caseapp.core.util.Formatter.DefaultNameFormatter]], converts
    * `camelCase` field names to `kebab-case` (`fooBar` is accepted as `--foo-bar`).
    */
  def nameFormatter(f: Formatter[Name]): Parser[T] =
    ParserWithNameFormatter(this, f)

  /** Creates a [[Parser]] accepting help / usage arguments, out of this one.
    *
    * The resulting parser accepts `--usage`, `--help`, `-h`, and `-help`, on top of the arguments
    * of this parser. See [[caseapp.core.help.WithHelp]].
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

  /** Creates a [[Parser]] accepting help / usage arguments and a full help argument, out of this
    * one.
    *
    * The resulting parser accepts the same arguments as [[withHelp]], and `--help-full`,
    * `--full-help`, `-help-full`, and `-full-help`. See [[caseapp.core.help.WithFullHelp]].
    */
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

  /** Creates a parser that transforms the result of this one with `f` */
  final def map[U](f: T => U): Parser[U] =
    MappedParser(this, f)

  /** Sets the origin of the arguments of this parser that don't have one yet
    *
    * The origin of an argument (see [[caseapp.core.Arg.origin]]) is a free-form string telling
    * where it comes from. Derived parsers set it to the name of the case class the argument comes
    * from.
    */
  def withDefaultOrigin(origin: String): Parser[T]
}

object Parser extends ParserCompanion with LowPriorityParserImplicits {

  /** Look for an implicit `Parser[T]` */
  def apply[T](implicit parser: Parser[T]): Parser[T] = parser

  /** An empty [[Parser]].
    *
    * Can be made non empty by successively calling `add` on it.
    */
  def nil: Parser[EmptyTuple] =
    NilParser

  /** Parser for `Option[T]`, that gives `None` rather than failing if `T` can't be built (if some
    * mandatory arguments are missing, for example)
    */
  implicit def option[T](implicit parser: Parser[T]): Parser[Option[T]] =
    OptionParser(parser)

  /** Parser for `Either[Error, T]`, that gives `Left` rather than failing if `T` can't be built (if
    * some mandatory arguments are missing, for example)
    */
  implicit def either[T](implicit parser: Parser[T]): Parser[Either[Error, T]] =
    EitherParser(parser)

  /** Adds methods to build parsers argument by argument, see [[ParserOps]] */
  implicit def toParserOps[T <: Tuple](parser: Parser[T]): ParserOps[T] =
    new ParserOps(parser)

}
