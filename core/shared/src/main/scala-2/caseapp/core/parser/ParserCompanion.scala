package caseapp.core.parser

import shapeless.{HList, HNil}

abstract class ParserCompanion {

  /** An empty [[Parser]].
    *
    * Can be made non empty by successively calling `add` on it.
    */
  def nil: Parser.Aux[HNil, HNil] =
    NilParser

  implicit def toParserOps[T <: HList, D <: HList](parser: Parser.Aux[T, D]): ParserOps[T, D] =
    new ParserOps(parser)

}
