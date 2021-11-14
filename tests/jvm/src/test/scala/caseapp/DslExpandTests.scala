package caseapp

import java.nio.file.Paths

import caseapp.core.Error
import caseapp.core.Error.SeveralErrors
import caseapp.core.help.{Help, WithHelp}
import caseapp.core.parser.PlatformArgsExpander
import caseapp.demo._
import shapeless.{Inl, Inr}
import utest._

object DslExpandTests extends TestSuite {

  import Definitions._

  val tests = TestSuite {

    test("handle expanded extra user arguments 1") {
      val argfile = Paths.get(DslExpandTests.getClass.getResource("/args1").toURI)
      val res     = Parser[NoArgs].detailedParse(PlatformArgsExpander.expand(List(s"@$argfile")))
      val expectedRes = Right((NoArgs(), RemainingArgs(Seq(), Seq("b", "-a", "--other"))))
      assert(res == expectedRes)
    }

    test("handle expanded extra user arguments 2") {
      val argfile = Paths.get(DslExpandTests.getClass.getResource("/args2").toURI)
      val res = Parser[NoArgs].detailedParse(PlatformArgsExpander.expand(List("--", s"@$argfile")))
      val expectedRes = Right((NoArgs(), RemainingArgs(Seq(), Seq("b", "-a", "--other"))))
      assert(res == expectedRes)
    }

  }
}
