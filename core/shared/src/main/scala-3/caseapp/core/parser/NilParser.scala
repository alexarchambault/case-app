package caseapp.core.parser

import caseapp.Name
import caseapp.core.{Arg, Error}
import caseapp.core.util.Formatter

case object NilParser extends Parser[EmptyTuple] {

  type D = EmptyTuple

  def init: D = EmptyTuple

  def step(
    args: List[String],
    index: Int,
    d: EmptyTuple,
    formatter: Formatter[Name]
  ): Right[(Error, Arg, List[String]), None.type] =
    Right(None)

  def get(d: D, formatter: Formatter[Name]): Right[Error, EmptyTuple] =
    Right(EmptyTuple)

  def args: Nil.type =
    scala.Nil

  def ::[A](argument: Argument[A]): ConsParser[A, EmptyTuple] =
    ConsParser[A, EmptyTuple](argument, this)

  def withDefaultOrigin(origin: String): Parser[EmptyTuple] =
    this
}
