package caseapp.cats

import caseapp.core.argparser.{ArgParser, SimpleArgParser}
import cats.data.NonEmptyList

object CatsArgParser {
  implicit def nonEmptyListArgParser[T]: ArgParser[NonEmptyList[T]] =
    SimpleArgParser.from[List[T]]("NonEmptyList") { s =>
      for {
        list <- implicitly[ArgParser[List[T]]].apply(None, s)
        nel <- NonEmptyList.fromList(list).toRight("Empty list !")
      } yield nel
    }
}
