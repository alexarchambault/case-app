package caseapp.core.parser

import caseapp.core.Error
import shapeless.HNil
import caseapp.core.util.Formatter
import caseapp.Name

case object NilParser extends Parser[HNil] {

  type D = HNil

  def init: D =
    HNil

  def step(
      args: List[String],
      d: HNil,
      formatter: Formatter[Name]
  ): Right[(Error, List[String]), None.type] =
    Right(None)

  def get(d: D, formatter: Formatter[Name]): Right[Error, HNil] =
    Right(HNil)

  def args: Nil.type =
    scala.Nil

}
