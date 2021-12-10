package caseapp.core.argparser

import caseapp.core.Error

final class MapErrorArgParser[T, U](
  argParser: ArgParser[T],
  from: U => T,
  to: T => Either[Error, U]
) extends ArgParser[U] {

  def apply(current: Option[U], index: Int, span: Int, value: String): Either[Error, U] =
    argParser(current.map(from), index, span, value).flatMap(to)

  override def optional(
    current: Option[U],
    index: Int,
    span: Int,
    value: String
  ): (Consumed, Either[Error, U]) = {
    val (consumed, res) = argParser.optional(current.map(from), index, span, value)
    val res0            = res.flatMap(to)
    (consumed, res0)
  }

  override def apply(current: Option[U], index: Int): Either[Error, U] =
    argParser(current.map(from), index).flatMap(to)

  override def isFlag: Boolean =
    argParser.isFlag

  def description: String =
    argParser.description

}
