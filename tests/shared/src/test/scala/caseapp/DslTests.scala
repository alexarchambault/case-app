package caseapp

import utest._

object DslTests extends TestSuite {

  final case class Result(foo: Int, bar: String = "ab", value: Double)

  val tests: Tests = Tests {

    "simple" - {

      val dslParser = Parser.nil
        .add[Int]("foo")
        .add[String]("bar", default = Some("ab"))
        .add[Double]("value")
        .as[Result]

      val tupledParser = Parser.nil
        .add[Int]("foo")
        .add[String]("bar", default = Some("ab"))
        .add[Double]("value")
        .tupled

      val derivedParser = Parser[Result]

      * - {
        val args = Seq("--foo", "2", "--bar", "bzz", "--value", "2.0")
        val dslRes = dslParser.parse(args)
        val derivedRes = derivedParser.parse(args)
        val expectedRes = Right((Result(2, "bzz", 2.0), Nil))
        assert(dslRes == expectedRes)
        assert(derivedRes == expectedRes)

        val expectedTupledRes = Right(((2, "bzz", 2.0), Nil))
        val tupledRes = tupledParser.parse(args)
        assert(tupledRes == expectedTupledRes)
      }

      * - {
        val args = Seq("--foo", "2", "--value", "2.0")
        val dslRes = dslParser.parse(args)
        val derivedRes = derivedParser.parse(args)
        val expectedRes = Right((Result(2, "ab", 2.0), Nil))
        assert(dslRes == expectedRes)
        assert(derivedRes == expectedRes)

        val expectedTupledRes = Right(((2, "ab", 2.0), Nil))
        val tupledRes = tupledParser.parse(args)
        assert(tupledRes == expectedTupledRes)
      }
    }
  }

}
