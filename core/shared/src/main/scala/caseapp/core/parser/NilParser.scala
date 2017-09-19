package caseapp.core.parser

import caseapp.core.Error
import shapeless.HNil

case object NilParser extends Parser[HNil] {

  override type D = HNil

  override def init: D =
    HNil

  override def step(args: List[String], d: HNil): Either[Error, None.type] =
    Right(None)

  override def get(d: D): Either[Seq[Error], HNil] =
    Right(HNil)

  override def args: Nil.type =
    scala.Nil

}
