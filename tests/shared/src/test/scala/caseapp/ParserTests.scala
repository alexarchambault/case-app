package caseapp

import caseapp.core.{Arg, Error, Indexed}
import caseapp.core.help.{WithFullHelp, WithHelp}
import caseapp.core.parser.{Argument, NilParser, StandardArgument}
import caseapp.core.parser.ParserOps
import caseapp.core.util.Formatter
import caseapp.core.Scala3Helpers._

import utest._

object ParserTests extends TestSuite {
  val tests = Tests {
    test("simple") {
      case class Helper(n: Int, value: String)
      val parser =
        Argument[Int](Arg("n")) ::
          Argument[String](Arg("value")) ::
          NilParser
      val res      = parser.to[Helper].parse(Seq("-n", "2", "something", "--value", "foo"))
      val expected = Right((Helper(2, "foo"), Seq("something")))
      assert(res == expected)
    }

    test("tuple") {
      case class Helper(n: Int, value: String)
      val parser =
        Argument[Int](Arg("n")) ::
          Argument[String](Arg("value")) ::
          NilParser
      val res      = parser.toTuple.parse(Seq("-n", "2", "something", "--value", "foo"))
      val expected = Right(((2, "foo"), Seq("something")))
      assert(res == expected)
    }
    test("default value") {
      case class Helper(n: Int, value: String)
      val parser =
        Argument[Int](Arg("n")) ::
          StandardArgument[String](Arg("value")).withDefault(() => Some("-")) ::
          NilParser
      test {
        val res      = parser.to[Helper].parse(Seq("-n", "2", "something", "--value", "foo"))
        val expected = Right((Helper(2, "foo"), Seq("something")))
        assert(res == expected)
      }
      test {
        val res      = parser.to[Helper].parse(Seq("-n", "2", "something"))
        val expected = Right((Helper(2, "-"), Seq("something")))
        assert(res == expected)
      }
      test {
        val res = parser.to[Helper].parse(Seq("--value", "other", "something"))
        assert(res.isLeft)
      }
    }
    test("Keep single dashes in user arguments") {
      case class Helper(n: Int, value: String)
      val parser =
        Argument[Int](Arg("n")) ::
          StandardArgument[String](Arg("value")).withDefault(() => Some("default")) ::
          NilParser
      test("single") {
        val res      = parser.to[Helper].parse(Seq("-n", "2", "-"))
        val expected = Right((Helper(2, "default"), Seq("-")))
        assert(res == expected)
      }
      test("several") {
        val res      = parser.to[Helper].parse(Seq("-", "a", "-", "-n", "2", "-"))
        val expected = Right((Helper(2, "default"), Seq("-", "a", "-", "-")))
        assert(res == expected)
      }
    }

    test("Retain origin class of options") {
      val parser: Parser[Definitions.MoreArgs] = Parser.derive
      val args                                 = parser.args
      val baseMap                              = args.groupBy(_.name.name)
      assert(baseMap.size == 3)
      assert(baseMap.forall(_._2.length == 1))
      val map = baseMap.map { case (k, v) => (k, v.head) }

      val countArg  = map.getOrElse("count", sys.error("count argument not found"))
      val valueArg  = map.getOrElse("value", sys.error("value argument not found"))
      val numFooArg = map.getOrElse("numFoo", sys.error("numFoo argument not found"))

      assert(countArg.origin == Some("MoreArgs"))
      assert(valueArg.origin == Some("FewArgs"))
      assert(numFooArg.origin == Some("FewArgs"))
    }

    test("WithHelp args have an origin") {
      case class Dummy()
      val parser: Parser[WithHelp[Dummy]] = WithHelp.parser
      val args                            = parser.args
      assert(args.nonEmpty)
      assert(args.forall(_.origin.contains("WithHelp")))
    }

    test("WithFullHelp args have an origin") {
      case class Dummy()
      val parser: Parser[WithFullHelp[Dummy]] = WithFullHelp.parser
      val args                                = parser.args
      assert(args.exists(_.origin.contains("WithHelp")))
      assert(args.exists(_.origin.contains("WithFullHelp")))
      assert(args.forall(_.origin.exists(o => o == "WithHelp" || o == "WithFullHelp")))
    }

    test("Custom Argument type") {
      case class Helper(n: Int, values: List[String])

      val valuesArgument = new Argument[List[String]] {
        def arg = Arg(Name("X")).withIsFlag(true)
        def withDefaultOrigin(origin: String): Argument[List[String]] = ???
        def init: Option[List[String]]                                = Some(Nil)
        def step(
          args: List[String],
          index: Int,
          d: Option[List[String]],
          nameFormatter: Formatter[Name]
        ): Either[(Error, List[String]), Option[(Option[List[String]], List[String])]] =
          args match {
            case h :: t if h.startsWith("-X") =>
              Right(Some((Some(h :: d.getOrElse(Nil)), t)))
            case _ => Right(None)
          }
        def get(
          d: Option[List[String]],
          nameFormatter: Formatter[Name]
        ): Either[Error, List[String]] =
          d.map(_.reverse).toRight(
            Error.RequiredOptionNotSpecified("-X*", Nil)
          )
      }

      val parser =
        Argument[Int](Arg("n")) ::
          valuesArgument ::
          NilParser

      val res      = parser.to[Helper].parse(Seq("-n", "2", "-Xa", "foo", "-Xb"))
      val expected = Right((Helper(2, List("-Xa", "-Xb")), List("foo")))
      assert(res == expected)
    }

    test("Indexed") {
      import Definitions.{FewArgsWithIndexed, WithIndexed}
      val parser = Parser[WithIndexed]
      test {
        val res = parser.parse(Seq("--aa", "foo"))
        val expected = Right((
          WithIndexed(aa = Some(Indexed(0, 1, true))),
          Seq("foo")
        ))
        assert(res == expected)
      }
      test {
        val res = parser.parse(Seq("foo", "--aa"))
        val expected = Right((
          WithIndexed(aa = Some(Indexed(1, 1, true))),
          Seq("foo")
        ))
        assert(res == expected)
      }
      test {
        val res = parser.parse(Seq("foo", "--aa", "--value", "foo"))
        val expected = Right((
          WithIndexed(
            aa = Some(Indexed(1, 1, true)),
            few = FewArgsWithIndexed(
              value = Indexed(2, 2, "foo")
            )
          ),
          Seq("foo")
        ))
        assert(res == expected)
      }
      test {
        val res = parser.parse(Seq("foo", "--num-foo=2", "--value", "foo"))
        val expected = Right((
          WithIndexed(
            few = FewArgsWithIndexed(
              numFoo = Indexed(1, 1, 2),
              value = Indexed(2, 2, "foo")
            )
          ),
          Seq("foo")
        ))
        assert(res == expected)
      }
      test {
        val res = parser.parse(Seq("--elem=a", "--elem", "b"))
        val expected = Right((
          WithIndexed(
            elem = List(
              Indexed(0, 1, "a"),
              Indexed(1, 2, "b")
            )
          ),
          Nil
        ))
        assert(res == expected)
      }
    }
  }
}
