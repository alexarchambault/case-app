package caseapp.core.argparser

import caseapp.core.Error
import dataclass.data

@data class FlagArgParser[T](
  description: String,
  parse: (Option[String], Int, Int) => Either[Error, T]
) extends ArgParser[T] {

  def apply(current: Option[T], index: Int, span: Int, value: String): Either[Error, T] =
    parse(Some(value), index, span)

  override def optional(
    current: Option[T],
    index: Int,
    span: Int,
    value: String
  ): (Consumed, Either[Error, T]) =
    (Consumed(false), parse(None, index, span))

  override def apply(current: Option[T], index: Int): Either[Error, T] =
    parse(None, index, 1)

  override def isFlag: Boolean =
    true

}

object FlagArgParser {

  def from[T](description: String)(parse: Option[String] => Either[Error, T]): FlagArgParser[T] =
    FlagArgParser(description, (valueOpt, _, _) => parse(valueOpt))

  private val trues  = Set("true", "1")
  private val falses = Set("false", "0")

  val unit: FlagArgParser[Unit] =
    from("flag") {
      case None =>
        Right(())
      case Some(s) =>
        if (trues(s))
          Right(())
        else if (falses(s))
          Left(Error.CannotBeDisabled)
        else
          Left(Error.UnrecognizedFlagValue(s))
    }

  val boolean: FlagArgParser[Boolean] =
    from("bool") {
      case None =>
        Right(true)
      case Some(s) =>
        if (trues(s))
          Right(true)
        else if (falses(s))
          Right(false)
        else
          Left(Error.UnrecognizedFlagValue(s))
    }

}
