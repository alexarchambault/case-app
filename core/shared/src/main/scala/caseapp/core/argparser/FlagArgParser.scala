package caseapp.core.argparser

import caseapp.core.Error

final case class FlagArgParser[T](
  description: String,
  parse: Option[String] => Either[Error, T]
) extends ArgParser[T] {

  def apply(current: Option[T], value: String): Either[Error, T] =
    parse(Some(value))

  override def optional(current: Option[T], value: String): Either[Error, (Consumed, T)] =
    parse(None)
      .right
      .map((Consumed(false), _))

  override def apply(current: Option[T]): Either[Error, T] =
    parse(None)

  override def isFlag: Boolean =
    true

}

object FlagArgParser {

  def from[T](description: String)(parse: Option[String] => Either[Error, T]): FlagArgParser[T] =
    FlagArgParser(description, parse)


  private val trues = Set("true", "1")
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
