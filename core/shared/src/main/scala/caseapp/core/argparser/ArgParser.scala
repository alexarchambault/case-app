package caseapp.core.argparser

import caseapp.@@
import caseapp.core.{Counter, Error}

/** Parses argument values of type `T`.
  *
  * Case class fields of type `T` need an implicit `ArgParser[T]`. Instances are provided for
  * standard types (numeric types, `String`, `Boolean`, `List`, `Option`…, see the implicit methods
  * of the companion object), and for some platform-specific ones (like `java.nio.file.Path` on the
  * JVM).
  *
  * Custom instances are most easily defined with [[SimpleArgParser.from]], or derived from an
  * existing instance with [[xmap]] or [[xmapError]]:
  * {{{
  * case class Port(value: Int)
  * object Port {
  *   implicit val argParser: ArgParser[Port] =
  *     SimpleArgParser.from("port") { s =>
  *       s.toIntOption.filter(_ > 0).map(Port(_))
  *         .toRight(Error.MalformedValue("port", s))
  *     }
  * }
  * }}}
  *
  * @tparam T:
  *   parsed value type
  */
abstract class ArgParser[T] {

  /** Parses a value.
    *
    * `value` must be consumed. Corresponds to cases like `--foo=bar`.
    *
    * @param current:
    *   latest parsed value wrapped in `Some` if any, `None` else
    * @param index:
    *   position of the option in the arguments
    * @param span:
    *   number of arguments the option and its value span: 1 for `--foo=bar`, 2 for `--foo bar`
    * @param value:
    *   `String` to parse
    * @return
    *   in case of success, a `T`, wrapped in `Right`; else, an error message, wrapped in
    *   [[caseapp.core.Error]] and `Left`
    */
  def apply(current: Option[T], index: Int, span: Int, value: String): Either[Error, T]

  /** Parses a value.
    *
    * Unlike `apply` above, `value` may or may not be consumed. Corresponds to cases like `--foo
    * bar`.
    *
    * Use of `value` or not must be returned via the [[caseapp.core.argparser.Consumed]] value.
    *
    * @param current:
    *   latest parsed value wrapped in `Some` if any, `None` else
    * @param index:
    *   position of the option in the arguments
    * @param span:
    *   number of arguments the option and its value span: 1 for `--foo=bar`, 2 for `--foo bar`
    * @param value:
    *   `String` to parse
    * @return
    *   in case of success, whether `value` was consumed and a `T`, wrapped in `Right`; else, an
    *   error message, wrapped in [[caseapp.core.Error]] and `Left`
    */
  def optional(
    current: Option[T],
    index: Int,
    span: Int,
    value: String
  ): (Consumed, Either[Error, T]) =
    (Consumed(true), apply(current, index, span, value))

  /** Called when the corresponding argument was specified with no value.
    *
    * Can happen if the option was enabled as very last argument, like `--bar` in `--foo 1 other
    * --bar`.
    *
    * @param current:
    *   latest parsed value wrapped in `Some` if any, `None` else
    * @param index:
    *   position of the option in the arguments
    * @return
    *   a `T` wrapped in `Right` in case of success, or an error message wrapped in
    *   [[caseapp.core.Error]] and `Left` else
    */
  def apply(current: Option[T], index: Int): Either[Error, T] =
    Left(Error.ArgumentMissing)

  /** Whether the parsed value corresponds to a flag.
    *
    * Prevents telling corresponding arguments expect a value in help messages.
    */
  def isFlag: Boolean =
    false

  /** Value description, like `int` or `string`
    *
    * Used in help messages, unless the corresponding field has a `ValueDescription` annotation.
    */
  def description: String

  /** Creates an `ArgParser[U]` out of this one
    *
    * @param from
    *   converts a previously parsed `U` back to a `T`
    * @param to
    *   converts a parsed `T` to a `U`
    */
  final def xmap[U](from: U => T, to: T => U): ArgParser[U] =
    new MapErrorArgParser[T, U](this, from, t => Right(to(t)))

  /** Creates an `ArgParser[U]` out of this one, with a conversion from `T` that can fail
    *
    * @param from
    *   converts a previously parsed `U` back to a `T`
    * @param to
    *   converts a parsed `T` to a `U`, or returns an error
    */
  final def xmapError[U](from: U => T, to: T => Either[Error, U]): ArgParser[U] =
    new MapErrorArgParser(this, from, to)

}

object ArgParser extends PlatformArgParsers {

  /** Look for an implicit `ArgParser[T]` */
  def apply[T](implicit parser: ArgParser[T]): ArgParser[T] = parser

  implicit def byte: ArgParser[Byte] =
    SimpleArgParser.byte

  implicit def short: ArgParser[Short] =
    SimpleArgParser.short

  implicit def int: ArgParser[Int] =
    SimpleArgParser.int

  implicit def long: ArgParser[Long] =
    SimpleArgParser.long

  implicit def double: ArgParser[Double] =
    SimpleArgParser.double

  implicit def float: ArgParser[Float] =
    SimpleArgParser.float

  implicit def bigDecimal: ArgParser[BigDecimal] =
    SimpleArgParser.bigDecimal

  implicit def string: ArgParser[String] =
    SimpleArgParser.string

  /** Flag that can't be disabled: accepts `--foo`, `--foo=true`, or `--foo=1` */
  implicit def unit: ArgParser[Unit] =
    FlagArgParser.unit

  /** Flag: `--foo` or `--foo=true` / `--foo=1` give `true`, `--foo=false` / `--foo=0` give `false`
    */
  implicit def boolean: ArgParser[Boolean] =
    FlagArgParser.boolean

  /** Flag counting how many times it is specified, see [[caseapp.core.Counter]] */
  implicit def counter: ArgParser[Int @@ Counter] =
    FlagAccumulatorArgParser.counter

  /** Option that can be specified several times, accumulating its values */
  implicit def list[T: ArgParser]: ArgParser[List[T]] =
    if (ArgParser[T].isFlag)
      FlagAccumulatorArgParser.list
    else
      AccumulatorArgParser.list

  /** Option that can be specified several times, accumulating its values */
  implicit def vector[T: ArgParser]: ArgParser[Vector[T]] =
    if (ArgParser[T].isFlag)
      FlagAccumulatorArgParser.vector
    else
      AccumulatorArgParser.vector

  /** Optional option, `None` if not specified (unless the field has another default value) */
  implicit def option[T: ArgParser]: ArgParser[Option[T]] =
    if (ArgParser[T].isFlag)
      FlagAccumulatorArgParser.option
    else
      AccumulatorArgParser.option

  /** Option that can be specified several times, keeping its last value, see [[Last]] */
  implicit def last[T: ArgParser]: ArgParser[Last[T]] =
    LastArgParser(implicitly)

}
