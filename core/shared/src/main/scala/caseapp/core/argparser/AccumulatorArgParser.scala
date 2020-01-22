package caseapp.core.argparser

import caseapp.core.Error

final case class AccumulatorArgParser[T](
  description: String,
  parse: (Option[T], String) => Either[Error, T]
) extends ArgParser[T] {

  def apply(current: Option[T], value: String): Either[Error, T] =
    parse(current, value)

}

object AccumulatorArgParser {

  def from[T](description: String)(parse: (Option[T], String) => Either[Error, T]): AccumulatorArgParser[T] =
    AccumulatorArgParser(description, parse)



  // FIXME (former comment, deprecated?) may not be fine with sequences/options of flags

  def list[T](implicit parser: ArgParser[T]): AccumulatorArgParser[List[T]] =
    from(parser.description + "*") { (prevOpt, s) =>
      parser(None, s).map { t =>
        // inefficient for big lists
        prevOpt.getOrElse(Nil) :+ t
      }
    }

  def vector[T](implicit parser: ArgParser[T]): AccumulatorArgParser[Vector[T]] =
    from(parser.description + "*") { (prevOpt, s) =>
      parser(None, s).map { t =>
        prevOpt.getOrElse(Vector.empty) :+ t
      }
    }

  def option[T](implicit parser: ArgParser[T]): AccumulatorArgParser[Option[T]] =
    from(parser.description + "?") { (prevOpt, s) =>
      parser(prevOpt.flatten, s).map(Some(_))
    }


}
