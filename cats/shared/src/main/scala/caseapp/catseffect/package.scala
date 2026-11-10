package caseapp

import caseapp.core.argparser.{AccumulatorArgParser, ArgParser}
import cats.data.NonEmptyList

/** [[https://typelevel.org/cats-effect/ cats-effect]] support for case-app, from the case-app-cats
  * module
  *
  * Provides [[IOCaseApp]], [[IOCommand]], and [[IOCommandsEntryPoint]], the cats-effect versions of
  * [[caseapp.core.app.CaseApp]], [[caseapp.core.app.Command]], and
  * [[caseapp.core.app.CommandsEntryPoint]].
  */
package object catseffect {

  /** Parser for `cats.data.NonEmptyList[T]`, for options that can be specified several times,
    * accumulating their values
    *
    * Unlike `List[T]` fields, such fields don't default to an empty value: unless they're given a
    * default value, they must be specified at least once.
    */
  implicit def nonEmptyListArgParser[T](
    implicit parser: ArgParser[T]
  ): AccumulatorArgParser[NonEmptyList[T]] =
    AccumulatorArgParser.from(parser.description + "*") { (prevOpt, idx, span, s) =>
      parser(None, idx, span, s).map { t =>
        // inefficient for big lists
        prevOpt.fold(NonEmptyList.one(t))(_ :+ t)
      }
    }
}
