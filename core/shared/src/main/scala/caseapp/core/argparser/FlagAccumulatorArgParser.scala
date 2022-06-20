package caseapp.core.argparser

import caseapp.core.{Counter, Error}
import caseapp.{@@, Tag}
import dataclass.data

@data case class FlagAccumulatorArgParser[T](
  description: String,
  parse: (Option[T], Int, Int, Option[String]) => Either[Error, T]
) extends ArgParser[T] {

  def apply(current: Option[T], index: Int, span: Int, value: String): Either[Error, T] =
    parse(current, index, span, Some(value))

  override def optional(
    current: Option[T],
    index: Int,
    span: Int,
    value: String
  ): (Consumed, Either[Error, T]) =
    (Consumed(false), parse(current, index, span, None))

  override def apply(current: Option[T], index: Int): Either[Error, T] =
    parse(current, index, 1, None)

  override def isFlag: Boolean =
    true

}

object FlagAccumulatorArgParser {

  def from[T](
    description: String
  )(
    parse: (Option[T], Int, Int, Option[String]) => Either[Error, T]
  ): FlagAccumulatorArgParser[T] =
    FlagAccumulatorArgParser(description, parse)

  val counter: FlagAccumulatorArgParser[Int @@ Counter] =
    from("counter") { (prevOpt, _, _, _) =>
      Right(Tag.of(prevOpt.fold(0)(Tag.unwrap) + 1))
    }

  def list[T](implicit parser: ArgParser[T]): FlagAccumulatorArgParser[List[T]] =
    from(parser.description + "*") { (prevOpt, idx, span, s) =>
      s.fold(parser(None, idx))(parser(None, idx, span, _)).map { t =>
        // inefficient for big lists
        prevOpt.getOrElse(Nil) :+ t
      }
    }

  def vector[T](implicit parser: ArgParser[T]): FlagAccumulatorArgParser[Vector[T]] =
    from(parser.description + "*") { (prevOpt, idx, span, s) =>
      s.fold(parser(None, idx))(parser(None, idx, span, _)).map { t =>
        prevOpt.getOrElse(Vector.empty) :+ t
      }
    }

  def option[T](implicit parser: ArgParser[T]): FlagAccumulatorArgParser[Option[T]] =
    from(parser.description + "?") { (prevOpt, idx, span, s) =>
      s.fold(parser(prevOpt.flatten, idx))(parser(prevOpt.flatten, idx, span, _))
        .map(Some(_))
    }

}
