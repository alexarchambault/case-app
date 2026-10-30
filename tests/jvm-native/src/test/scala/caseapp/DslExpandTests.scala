package caseapp

import caseapp.core.Indexed
import caseapp.core.parser.PlatformArgsExpander
import utest._

object DslExpandTests extends TestSuite {

  import Definitions._

  val tests = TestSuite {

    test("handle expanded extra user arguments 1") {
      val argfile                = TestResources.path("args1")
      val parser: Parser[NoArgs] = Parser.derive
      val res = parser.detailedParse(PlatformArgsExpander.expand(List(s"@$argfile")))
      val expectedRes = Right((
        NoArgs(),
        RemainingArgs(Seq(), Seq(Indexed(1, 1, "b"), Indexed(2, 1, "-a"), Indexed(3, 1, "--other")))
      ))
      assert(res == expectedRes)
    }

    test("handle expanded extra user arguments 2") {
      val argfile                = TestResources.path("args2")
      val parser: Parser[NoArgs] = Parser.derive
      val res = parser.detailedParse(PlatformArgsExpander.expand(List("--", s"@$argfile")))
      val expectedRes = Right((
        NoArgs(),
        RemainingArgs(
          Seq(),
          Seq(
            Indexed(1, 1, "b"),
            Indexed(2, 1, "-a"),
            Indexed(3, 1, "--other")
          )
        )
      ))
      assert(res == expectedRes)
    }

  }
}
