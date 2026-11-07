package caseapp.core.argparser

import caseapp.core.Error
import dataclass.data

/** [[ArgParser]] for options that require a value, and can't be specified more than once
  *
  * Specifying such an option more than once fails with [[caseapp.core.Error.AlreadySpecified]]. Use
  * `List[T]` or [[Last]] fields to accept options several times.
  *
  * @param description
  *   value description, used in help messages (see [[ArgParser.description]])
  * @param parse
  *   parses a value, given the value, the position of the option in the arguments, and the number
  *   of arguments the option and its value span (see [[ArgParser.apply]])
  */
@data(
  deprecatedSetters = true,
  deprecatedSettersMessage = "Use copy instead",
  deprecatedSettersSince = "2.1.2"
) case class SimpleArgParser[T](
  description: String,
  parse: (String, Int, Int) => Either[Error, T]
) extends ArgParser[T] {

  def apply(current: Option[T], index: Int, span: Int, value: String): Either[Error, T] =
    current match {
      case None =>
        parse(value, index, span)
      case Some(_) =>
        Left(Error.AlreadySpecified)
    }

}

object SimpleArgParser {

  /** Creates a [[SimpleArgParser]] out of a function parsing a `String`
    *
    * @param description
    *   value description, used in help messages, like `int` or `path`
    * @param parse
    *   parses a value, returning an error in case of failure (like
    *   [[caseapp.core.Error.MalformedValue]] or [[caseapp.core.Error.Other]])
    */
  def from[T](description: String)(parse: String => Either[Error, T]): SimpleArgParser[T] =
    SimpleArgParser(description, (value, _, _) => parse(value))

  /** Parser for `Byte` */
  val byte: SimpleArgParser[Byte] =
    from("byte") { s =>
      try Right(s.toByte)
      catch {
        case _: NumberFormatException =>
          Left(Error.MalformedValue("byte-sized integer", s))
      }
    }

  /** Parser for `Short` */
  val short: SimpleArgParser[Short] =
    from("short") { s =>
      try Right(s.toShort)
      catch {
        case _: NumberFormatException =>
          Left(Error.MalformedValue("short integer", s))
      }
    }

  /** Parser for `Int` */
  val int: SimpleArgParser[Int] =
    from("int") { s =>
      try Right(s.toInt)
      catch {
        case _: NumberFormatException =>
          Left(Error.MalformedValue("integer", s))
      }
    }

  /** Parser for `Long` */
  val long: SimpleArgParser[Long] =
    from("long") { s =>
      try Right(s.toLong)
      catch {
        case _: NumberFormatException =>
          Left(Error.MalformedValue("long integer", s))
      }
    }

  /** Parser for `Double` */
  val double: SimpleArgParser[Double] =
    from("double") { s =>
      try Right(s.toDouble)
      catch {
        case _: NumberFormatException =>
          Left(Error.MalformedValue("double float", s))
      }
    }

  /** Parser for `Float` */
  val float: SimpleArgParser[Float] =
    from("float") { s =>
      try Right(s.toFloat)
      catch {
        case _: NumberFormatException =>
          Left(Error.MalformedValue("float", s))
      }
    }

  /** Parser for `BigDecimal` */
  val bigDecimal: SimpleArgParser[BigDecimal] =
    from("decimal") { s =>
      try Right(BigDecimal(s))
      catch {
        case _: NumberFormatException =>
          Left(Error.MalformedValue("decimal", s))
      }
    }

  /** Parser for `String` (any value is accepted) */
  val string: SimpleArgParser[String] =
    from("string")(Right(_))

}
