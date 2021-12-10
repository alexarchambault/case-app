package caseapp.core.argparser

import caseapp.core.Error
import dataclass.data

@data class SimpleArgParser[T](
  description: String,
  parse: (String, Int, Int) => Either[Error, T]
) extends ArgParser[T] {

  def apply(current: Option[T], index: Int, span: Int, value: String): Either[Error, T] =
    current match {
      case None =>
        parse(value, index, span)
      case Some(_) =>
        Left(Error.ArgumentAlreadySpecified("???"))
    }

}

object SimpleArgParser {

  def from[T](description: String)(parse: String => Either[Error, T]): SimpleArgParser[T] =
    SimpleArgParser(description, (value, _, _) => parse(value))

  val int: SimpleArgParser[Int] =
    from("int") { s =>
      try Right(s.toInt)
      catch {
        case _: NumberFormatException =>
          Left(Error.MalformedValue("integer", s))
      }
    }

  val long: SimpleArgParser[Long] =
    from("long") { s =>
      try Right(s.toLong)
      catch {
        case _: NumberFormatException =>
          Left(Error.MalformedValue("long integer", s))
      }
    }

  val double: SimpleArgParser[Double] =
    from("double") { s =>
      try Right(s.toDouble)
      catch {
        case _: NumberFormatException =>
          Left(Error.MalformedValue("double float", s))
      }
    }

  val float: SimpleArgParser[Float] =
    from("float") { s =>
      try Right(s.toFloat)
      catch {
        case _: NumberFormatException =>
          Left(Error.MalformedValue("float", s))
      }
    }

  val bigDecimal: SimpleArgParser[BigDecimal] =
    from("decimal") { s =>
      try Right(BigDecimal(s))
      catch {
        case _: NumberFormatException =>
          Left(Error.MalformedValue("decimal", s))
      }
    }

  val string: SimpleArgParser[String] =
    from("string")(Right(_))

}
