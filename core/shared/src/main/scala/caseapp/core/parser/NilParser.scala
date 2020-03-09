package caseapp.core.parser

import caseapp.core.Error
import shapeless.HNil
import caseapp.core.util.OptionFormatter

case object NilParser extends Parser[HNil] {

  type D = HNil

  def init: D =
    HNil

  def step(
      args: List[String],
      d: HNil,
      formatter: OptionFormatter
  ): Right[(Error, List[String]), None.type] =
    Right(None)

  def get(d: D, formatter: OptionFormatter): Right[Error, HNil] =
    Right(HNil)

  def args: Nil.type =
    scala.Nil

}
