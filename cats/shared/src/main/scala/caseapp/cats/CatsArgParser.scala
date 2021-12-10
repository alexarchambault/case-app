package caseapp.cats

import caseapp.core.argparser.{AccumulatorArgParser, ArgParser}
import cats.data.NonEmptyList

object CatsArgParser {
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
