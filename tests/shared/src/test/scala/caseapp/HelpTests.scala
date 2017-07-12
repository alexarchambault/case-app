package caseapp

import caseapp.core.Messages
import utest._

object HelpTests extends TestSuite {

  import Definitions._

  case class Options(
    first: Int,
    @Hidden
      second: String
  )

  val tests = TestSuite {

    "generate a help message" - {

      val message = CaseApp.helpMessage[Example]

      val expectedMessage =
        """Example
          |Usage: example [options]
          |  --foo  <value>
          |  --bar  <value>""".stripMargin

      def lines(s: String) = s.lines.toVector

      for (((a, b), idx) <- lines(message).zip(lines(expectedMessage)).zipWithIndex if a != b)
        Console.err.println(s"Line $idx, expected '$b', got '$a'")

      val res = lines(message)
      val expectedRes = lines(expectedMessage)
      assert(res == expectedRes)
    }

    "don't add a help message for fields annotated with @Hidden" - {

      val helpLines = Messages[Options].helpMessage.lines.toVector

      * - {
        val res = helpLines.count(_.contains("--first"))
        val expectedRes = 1
        assert(res == expectedRes)
      }
      * - {
        val res = helpLines.count(_.contains("--second"))
        val expectedRes = 0
        assert(res == expectedRes)
      }
    }

  }

}
