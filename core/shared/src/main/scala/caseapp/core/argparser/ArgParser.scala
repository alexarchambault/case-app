package caseapp.core.argparser

import caseapp.@@
import caseapp.core.{Counter, Error}

/**
  * Parses argument values of type `T`.
  *
  * @tparam T: parsed value type
  */
abstract class ArgParser[T] {

  /**
    * Parses a value.
    *
    * `value` must be consumed. Corresponds to cases like `--foo=bar`.
    *
    * @param current: latest parsed value wrapped in [[scala.Some]] if any, [[scala.None]] else
    * @param value: [[scala.Predef.String]] to parse
    * @return in case of success, a `T`, wrapped in [[scala.Right]]; else, and error message, wrapped in [[caseapp.core.Error]] and [[scala.Left]]
    */
  def apply(current: Option[T], value: String): Either[Error, T]

  /**
    * Parses a value.
    *
    * Unlike `apply` above, `value` may or may not be consumed. Corresponds to cases like `--foo bar`.
    *
    * Use of `value` or not must be returned via the [[caseapp.core.argparser.Consumed]] value.
    *
    * @param current: latest parsed value wrapped in [[scala.Some]] if any, [[scala.None]] else
    * @param value: [[scala.Predef.String]] to parse
    * @return in case of success, whether `value` was consumed and a `T`, wrapped in [[scala.Right]]; else, and error message, wrapped in [[caseapp.core.Error]] and [[scala.Left]]
    */
  def optional(current: Option[T], value: String): (Consumed, Either[Error, T]) =
    (Consumed(true), apply(current, value))

  /**
    * Called when the corresponding argument was specific with no value.
    *
    * Can happen if the option was enabled as very last argument, like `--bar` in `--foo 1 other --bar`.
    *
    * @param current: latest parsed value wrapped in [[scala.Some]] if any, [[scala.None]] else
    * @return a `T` wrapped in [[scala.Right]] in case of success, or an error message wrapped in [[caseapp.core.Error]] and [[scala.Left]] else
    */
  def apply(current: Option[T]): Either[Error, T] =
    Left(Error.ArgumentMissing)

  /**
    * Whether the parsed value corresponds to a flag.
    *
    * Prevents telling corresponding arguments expect a value in help messages.
    */
  def isFlag: Boolean =
    false

  /**
    * Value description.
    *
    * Used in help messages.
    */
  def description: String


  final def xmap[U](from: U => T, to: T => U): ArgParser[U] =
    new MapErrorArgParser[T, U](this, from, t => Right(to(t)))
  final def xmapError[U](from: U => T, to: T => Either[Error, U]): ArgParser[U] =
    new MapErrorArgParser(this, from, to)

}

object ArgParser extends PlatformArgParsers {

  /** Look for an implicit `ArgParser[T]` */
  def apply[T](implicit parser: ArgParser[T]): ArgParser[T] = parser


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

  implicit def unit: ArgParser[Unit] =
    FlagArgParser.unit

  implicit def boolean: ArgParser[Boolean] =
    FlagArgParser.boolean

  implicit def counter: ArgParser[Int @@ Counter] =
    FlagAccumulatorArgParser.counter

  implicit def list[T: ArgParser]: ArgParser[List[T]] =
    AccumulatorArgParser.list

  implicit def vector[T: ArgParser]: ArgParser[Vector[T]] =
    AccumulatorArgParser.vector

  implicit def option[T: ArgParser]: ArgParser[Option[T]] =
    AccumulatorArgParser.option

  implicit def last[T: ArgParser]: ArgParser[Last[T]] =
    LastArgParser(implicitly)

}

