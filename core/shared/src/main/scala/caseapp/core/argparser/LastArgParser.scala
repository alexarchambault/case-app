package caseapp.core.argparser

import caseapp.core.Error
import dataclass.data

@data class LastArgParser[T](parser: ArgParser[T]) extends ArgParser[Last[T]] {

  def apply(current: Option[Last[T]], value: String): Either[Error, Last[T]] =
    parser(None, value).map(Last(_))

  override def optional(
    current: Option[Last[T]],
    value: String
  ): (Consumed, Either[Error, Last[T]]) = {
    val (consumed, res) = parser.optional(None, value)
    val res0            = res.map(t => Last(t))
    (consumed, res0)
  }

  override def apply(current: Option[Last[T]]): Either[Error, Last[T]] =
    parser(None).map(Last(_))

  override def isFlag: Boolean =
    parser.isFlag

  def description: String =
    parser.description

}
