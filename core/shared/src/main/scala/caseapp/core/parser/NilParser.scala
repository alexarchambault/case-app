package caseapp.core.parser

import caseapp.core.Error
import shapeless.HNil

case object NilParser extends Parser[HNil] {

  type D = HNil

  def init: D =
    HNil

  def step(args: List[String], d: HNil): Either[Error, None.type] =
    Right(None)

  def get(d: D): Either[Error, HNil] =
    Right(HNil)

  def args: Nil.type =
    scala.Nil

}
