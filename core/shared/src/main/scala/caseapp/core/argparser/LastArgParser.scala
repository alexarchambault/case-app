package caseapp.core.argparser

import caseapp.core.Error

final case class LastArgParser[T](parser: ArgParser[T]) extends ArgParser[Last[T]] {

  def apply(current: Option[Last[T]], value: String): Either[Error, Last[T]] =
    parser(None, value)
      .right
      .map(Last(_))

  override def optional(current: Option[Last[T]], value: String): Either[Error, (Consumed, Last[T])] =
    parser
      .optional(None, value)
      .right
      .map {
        case (consumed, t) =>
          (consumed, Last(t))
      }

  override def apply(current: Option[Last[T]]): Either[Error, Last[T]] =
    parser(None)
      .right
      .map(Last(_))

  override def isFlag: Boolean =
    parser.isFlag

  def description: String =
    parser.description

}
