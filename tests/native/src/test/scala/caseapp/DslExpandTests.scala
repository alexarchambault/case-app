package caseapp

import java.nio.file.Paths

import caseapp.core.Indexed
import caseapp.core.parser.PlatformArgsExpander
import utest._

object DslExpandTests extends TestSuite {

  import Definitions._

  def sbv = NativeUtil.scalaBinaryVersion

  private lazy val testResourceDir =
    Option(System.getenv("CASEAPP_NATIVE_TESTS_RESOURCES"))
      .map(os.Path(_))
      .getOrElse {
        sys.error("CASEAPP_NATIVE_TESTS_RESOURCES not set")
      }

  val tests = TestSuite {

    test("handle expanded extra user arguments 1") {
      val parser: Parser[NoArgs] = Parser.derive
      val res = parser.detailedParse(
        PlatformArgsExpander.expand(List(s"@$testResourceDir/args1"))
      )
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

    test("handle expanded extra user arguments 2") {
      val parser: Parser[NoArgs] = Parser.derive
      val res = parser.detailedParse(PlatformArgsExpander.expand(List(
        "--",
        s"@$testResourceDir/args2"
      )))
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
