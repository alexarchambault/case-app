package caseapp

import java.nio.file.Paths

import caseapp.core.Indexed
import caseapp.core.parser.PlatformArgsExpander
import caseapp.demo._
import utest._

object DslExpandTests extends TestSuite {

  import Definitions._

  val tests = TestSuite {

    test("handle expanded extra user arguments 1") {
      val argfile                = Paths.get(DslExpandTests.getClass.getResource("/args1").toURI)
      val parser: Parser[NoArgs] = Parser.derive
      val res = parser.detailedParse(PlatformArgsExpander.expand(List(s"@$argfile")))
      val expectedRes = Right((
        NoArgs(),
        RemainingArgs(Seq(), Seq(Indexed(1, 1, "b"), Indexed(2, 1, "-a"), Indexed(3, 1, "--other")))
      ))
      assert(res == expectedRes)
    }

    test("handle expanded extra user arguments 2") {
      val argfile                = Paths.get(DslExpandTests.getClass.getResource("/args2").toURI)
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
