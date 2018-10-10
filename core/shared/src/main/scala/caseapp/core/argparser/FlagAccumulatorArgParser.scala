package caseapp.core.argparser

import caseapp.core.{Counter, Error}
import caseapp.{@@, Tag}

final case class FlagAccumulatorArgParser[T](
  description: String,
  parse: (Option[T], Option[String]) => Either[Error, T]
) extends ArgParser[T] {

  def apply(current: Option[T], value: String): Either[Error, T] =
    parse(current, Some(value))

  override def optional(current: Option[T], value: String): (Consumed, Either[Error, T]) =
    (Consumed(false), parse(current, None))

  override def apply(current: Option[T]): Either[Error, T] =
    parse(current, None)

  override def isFlag: Boolean =
    true

}

object FlagAccumulatorArgParser {

  def from[T](description: String)(parse: (Option[T], Option[String]) => Either[Error, T]): FlagAccumulatorArgParser[T] =
    FlagAccumulatorArgParser(description, parse)


  val counter: FlagAccumulatorArgParser[Int @@ Counter] =
    from("counter") { (prevOpt, _) =>
      Right(Tag.of(prevOpt.fold(0)(Tag.unwrap) + 1))
    }

}
