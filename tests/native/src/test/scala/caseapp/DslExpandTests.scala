package caseapp

import java.nio.file.Paths

import caseapp.core.parser.PlatformArgsExpander
import utest._

object DslExpandTests extends TestSuite {

  import Definitions._

  val tests = TestSuite {

    "handle expanded extra user arguments 1" - {
      val res = Parser[NoArgs].detailedParse(PlatformArgsExpander.expand(List(s"@./tests/native/target/scala-2.11/test-classes/args1")))
      val expectedRes = Right((NoArgs(), RemainingArgs(Seq(), Seq("b", "-a", "--other"))))
      assert(res == expectedRes)
    }

    "handle expanded extra user arguments 2" - {
      val res = Parser[NoArgs].detailedParse(PlatformArgsExpander.expand(List("--", s"@./tests/native/target/scala-2.11/test-classes/args2")))
      val expectedRes = Right((NoArgs(), RemainingArgs(Seq(), Seq("b", "-a", "--other"))))
      assert(res == expectedRes)
    }

  }
}
