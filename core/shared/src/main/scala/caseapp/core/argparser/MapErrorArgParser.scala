package caseapp.core.argparser

import caseapp.core.Error

final class MapErrorArgParser[T, U](argParser: ArgParser[T], from: U => T, to: T => Either[Error, U]) extends ArgParser[U] {

  def apply(current: Option[U], value: String): Either[Error, U] =
    argParser(current.map(from), value)
      .right
      .flatMap(to)

  override def optional(current: Option[U], value: String): Either[Error, (Consumed, U)] =
    argParser.optional(current.map(from), value)
      .right.flatMap {
        case (c, t) =>
          to(t)
            .right.map((c, _))
      }

  override def apply(current: Option[U]): Either[Error, U] =
    argParser(current.map(from))
      .right.flatMap(to)

  override def isFlag: Boolean =
    argParser.isFlag

  def description: String =
    argParser.description

}
