package caseapp

import caseapp.core.Arg
import caseapp.core.parser.{Argument, NilParser}
import utest._
import caseapp.core.parser.ParserOps

object ParserTests extends TestSuite {
  val tests = Tests {
    test("simple") {
      case class Helper(n: Int, value: String)
      val parser =
        Argument[Int](Arg("n")) ::
        Argument[String](Arg("value")) ::
          NilParser
      val res = parser.to[Helper].parse(Seq("-n", "2", "something", "--value", "foo"))
      val expected = Right((Helper(2, "foo"), Seq("something")))
    }
    test("tuple") {
      case class Helper(n: Int, value: String)
      val parser =
        Argument[Int](Arg("n")) ::
        Argument[String](Arg("value")) ::
          NilParser
      val res = parser.toTuple.parse(Seq("-n", "2", "something", "--value", "foo"))
      val expected = Right(((2, "foo"), Seq("something")))
    }
    test("default value") {
      case class Helper(n: Int, value: String)
      val parser =
        Argument[Int](Arg("n")) ::
        Argument[String](Arg("value")).withDefault(() => Some("-")) ::
          NilParser
      test {
        val res = parser.to[Helper].parse(Seq("-n", "2", "something", "--value", "foo"))
        val expected = Right((Helper(2, "foo"), Seq("something")))
      }
      test {
        val res = parser.to[Helper].parse(Seq("-n", "2", "something"))
        val expected = Right((Helper(2, "-"), Seq("something")))
      }
      test {
        val res = parser.to[Helper].parse(Seq("--value", "other", "something"))
        assert(res.isLeft)
      }
    }
  }
}
