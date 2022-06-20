package caseapp.core.argparser

import caseapp.core.Error
import dataclass.data

@data case class LastArgParser[T](parser: ArgParser[T]) extends ArgParser[Last[T]] {

  def apply(
    current: Option[Last[T]],
    index: Int,
    span: Int,
    value: String
  ): Either[Error, Last[T]] =
    parser(None, index, span, value).map(Last(_))

  override def optional(
    current: Option[Last[T]],
    index: Int,
    span: Int,
    value: String
  ): (Consumed, Either[Error, Last[T]]) = {
    val (consumed, res) = parser.optional(None, index, span, value)
    val res0            = res.map(t => Last(t))
    (consumed, res0)
  }

  override def apply(current: Option[Last[T]], index: Int): Either[Error, Last[T]] =
    parser(None, index).map(Last(_))

  override def isFlag: Boolean =
    parser.isFlag

  def description: String =
    parser.description

}
