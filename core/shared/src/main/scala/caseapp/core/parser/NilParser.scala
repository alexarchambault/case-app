package caseapp.core.parser

import caseapp.Name
import caseapp.core.{Arg, Error}
import caseapp.core.util.Formatter
import shapeless.HNil

case object NilParser extends Parser[HNil] {

  type D = HNil

  def init: D =
    HNil

  def step(
      args: List[String],
      d: HNil,
      formatter: Formatter[Name]
  ): Right[(Error, Arg, List[String]), None.type] =
    Right(None)

  def get(d: D, formatter: Formatter[Name]): Right[Error, HNil] =
    Right(HNil)

  def args: Nil.type =
    scala.Nil

  def ::[A](argument: Argument[A]): ConsParser[A, HNil, HNil] =
    ConsParser[A, HNil, HNil](
      argument.arg,
      argument.argParser,
      argument.default,
      this
    )

}
