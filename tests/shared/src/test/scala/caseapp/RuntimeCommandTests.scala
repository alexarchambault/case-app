package caseapp

import caseapp.demo._
import caseapp.Definitions._
import caseapp.core.commandparser.RuntimeCommandParser
import shapeless.{Inl, Inr}
import utest._

object RuntimeCommandTests extends TestSuite {

  val tests = Tests {
    test("not adt") {

      val commands = Map(
        List("c1") -> ManualCommandNotAdtStuff.Command1,
        List("c2") -> ManualCommandNotAdtStuff.Command2,
        List("c3") -> ManualCommandNotAdtStuff.Command3StopAtUnreco,
        List("c4") -> ManualCommandNotAdtStuff.Command4NameFormatter,
        List("c5") -> ManualCommandNotAdtStuff.Command5IgnoreUnrecognized
      )

      test {
        val res = RuntimeCommandParser.parse(commands, List("c1", "-s", "aa"))
        val expectedRes = Some((List("c1"), ManualCommandNotAdtStuff.Command1, List("-s", "aa")))
        assert(res == expectedRes)
      }

      test {
        val res = RuntimeCommandParser.parse(commands, List("c2", "-b"))
        val expectedRes = Some((List("c2"), ManualCommandNotAdtStuff.Command2, List("-b")))
        assert(res == expectedRes)
      }
    }

    test("sub commands") {
      val commands = Map(
        List("foo") -> ManualSubCommandStuff.Command1,
        List("foo", "list") -> ManualSubCommandStuff.Command2
      )
      test {
        val res = RuntimeCommandParser.parse(commands, List("foo", "-s", "aa"))
        val expectedRes = Some((List("foo"), ManualSubCommandStuff.Command1, List("-s", "aa")))
        assert(res == expectedRes)
      }

      test {
        val res = RuntimeCommandParser.parse(commands, List("foo", "list", "-b"))
        val expectedRes = Some((List("foo", "list"), ManualSubCommandStuff.Command2, List("-b")))
        assert(res == expectedRes)
      }
    }
  }

}
