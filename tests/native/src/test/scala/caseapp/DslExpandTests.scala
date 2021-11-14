package caseapp

import java.nio.file.Paths

import caseapp.core.parser.PlatformArgsExpander
import utest._

object DslExpandTests extends TestSuite {

  import Definitions._

  def sbv = NativeUtil.scalaBinaryVersion

  val tests = TestSuite {

    test("handle expanded extra user arguments 1") {
      val res = Parser[NoArgs].detailedParse(
        PlatformArgsExpander.expand(List(s"@./tests/native/target/scala-$sbv/test-classes/args1"))
      )
      val expectedRes = Right((NoArgs(), RemainingArgs(Seq(), Seq("b", "-a", "--other"))))
      assert(res == expectedRes)
    }

    test("handle expanded extra user arguments 2") {
      val res = Parser[NoArgs].detailedParse(PlatformArgsExpander.expand(List(
        "--",
        s"@./tests/native/target/scala-$sbv/test-classes/args2"
      )))
      val expectedRes = Right((NoArgs(), RemainingArgs(Seq(), Seq("b", "-a", "--other"))))
      assert(res == expectedRes)
    }

  }
}
