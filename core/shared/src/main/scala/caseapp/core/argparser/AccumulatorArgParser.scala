package caseapp.core.argparser

import caseapp.core.Error
import dataclass.data

@data case class AccumulatorArgParser[T](
  description: String,
  parse: (Option[T], Int, Int, String) => Either[Error, T]
) extends ArgParser[T] {

  def apply(current: Option[T], index: Int, span: Int, value: String): Either[Error, T] =
    parse(current, index, span, value)

}

object AccumulatorArgParser {

  def from[T](
    description: String
  )(
    parse: (Option[T], Int, Int, String) => Either[Error, T]
  ): AccumulatorArgParser[T] =
    AccumulatorArgParser(description, parse)

  // FIXME (former comment, deprecated?) may not be fine with sequences/options of flags

  def list[T](implicit parser: ArgParser[T]): AccumulatorArgParser[List[T]] =
    from(parser.description + "*") { (prevOpt, idx, span, s) =>
      parser(None, idx, span, s).map { t =>
        // inefficient for big lists
        prevOpt.getOrElse(Nil) :+ t
      }
    }

  def vector[T](implicit parser: ArgParser[T]): AccumulatorArgParser[Vector[T]] =
    from(parser.description + "*") { (prevOpt, idx, span, s) =>
      parser(None, idx, span, s).map { t =>
        prevOpt.getOrElse(Vector.empty) :+ t
      }
    }

  def option[T](implicit parser: ArgParser[T]): AccumulatorArgParser[Option[T]] =
    from(parser.description + "?") { (prevOpt, idx, span, s) =>
      parser(prevOpt.flatten, idx, span, s).map(Some(_))
    }

}
