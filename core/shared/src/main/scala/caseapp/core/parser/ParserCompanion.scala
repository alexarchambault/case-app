package caseapp.core.parser

import caseapp.core.{Arg, Error}

abstract class ParserCompanion {

  sealed abstract class Step extends Product with Serializable {
    def index: Int
    def consumed: Int
  }
  object Step {
    sealed abstract class SingleArg extends Step {
      final def consumed: Int = 1
    }
    final case class DoubleDash(index: Int)                                      extends SingleArg
    final case class IgnoredUnrecognized(index: Int)                             extends SingleArg
    final case class FirstUnrecognized(index: Int, isOption: Boolean)            extends SingleArg
    final case class Unrecognized(index: Int, error: Error.UnrecognizedArgument) extends SingleArg
    final case class StandardArgument(index: Int)                                extends SingleArg
    final case class MatchedOption(index: Int, consumed: Int, arg: Arg)          extends Step
    final case class ErroredOption(index: Int, consumed: Int, arg: Arg, error: Error) extends Step
  }

  def consumed(initial: List[String], updated: List[String]): Int =
    initial match {
      case _ :: tail if tail eq updated      => 1
      case _ :: _ :: tail if tail eq updated => 2
      case _                                 =>
        initial.length - updated.length // kind of meh, might make parsing O(args.length^2)
    }

}
